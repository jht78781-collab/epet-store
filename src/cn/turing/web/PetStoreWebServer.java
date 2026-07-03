package cn.turing.web;

import cn.turing.dao.PetDao;
import cn.turing.dao.PetOwnerDao;
import cn.turing.dao.impl.PetDaoImpl;
import cn.turing.dao.impl.PetOwnerDaoImpl;
import cn.turing.entity.Pet;
import cn.turing.entity.PetOwner;
import cn.turing.entity.PetStore;
import cn.turing.manager.impl.PetOwnerServiceImpl;
import cn.turing.manager.impl.PetStoreServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量 Web 入口：用 JDK 自带 HttpServer 提供页面和 JSON API。
 */
public class PetStoreWebServer {
    private static final Pattern JSON_FIELD = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|-?\\d+)");
    private static final String CORS_ORIGINS = config("epet.cors.origins", "EPET_CORS_ORIGINS", "*");

    private final Path webRoot;

    public PetStoreWebServer(Path webRoot) {
        this.webRoot = webRoot;
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(config("epet.web.port", "PORT", "8080"));
        String host = config("epet.web.host", "EPET_WEB_HOST", "0.0.0.0");
        Path webRoot = Path.of(System.getProperty("epet.web.root", "web")).toAbsolutePath().normalize();

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        PetStoreWebServer app = new PetStoreWebServer(webRoot);
        server.createContext("/api/health", app::handleHealth);
        server.createContext("/api/login", app::handleLogin);
        server.createContext("/api/pets", app::handlePets);
        server.createContext("/api/stores", app::handleStores);
        server.createContext("/api/owner", app::handleOwner);
        server.createContext("/api/buy", app::handleBuy);
        server.createContext("/", app::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("宠物商店前端已启动：http://127.0.0.1:" + port + "/");
        System.out.println("监听地址：" + host + ":" + port);
        System.out.println("静态资源目录：" + webRoot);
        Thread.currentThread().join();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) {
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) {
            return;
        }

        try {
            Map<String, String> request = parseJsonObject(readBody(exchange));
            String name = trimToEmpty(request.get("name"));
            String password = trimToEmpty(request.get("password"));
            if (name.isEmpty() || password.isEmpty()) {
                sendApiError(exchange, 400, "请输入用户名和密码");
                return;
            }

            PetOwner owner = new PetOwnerServiceImpl().login(name, password);
            if (owner == null) {
                sendApiError(exchange, 401, "用户名或密码不正确");
                return;
            }

            sendJson(exchange, 200, "{\"owner\":" + ownerJson(owner) + "}");
        } catch (Exception ex) {
            sendServerError(exchange, ex);
        }
    }

    private void handlePets(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) {
            return;
        }

        try {
            Map<String, String> query = queryParams(exchange.getRequestURI().getRawQuery());
            int storeId = parseOptionalInt(query.get("storeId"), 0);
            PetStoreServiceImpl storeService = new PetStoreServiceImpl();
            List<Pet> pets = storeService.getPetsInstock(storeId);
            sendJson(exchange, 200, "{\"pets\":" + petsJson(pets, storeService) + "}");
        } catch (Exception ex) {
            sendServerError(exchange, ex);
        }
    }

    private void handleStores(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) {
            return;
        }

        try {
            List<PetStore> stores = new PetStoreServiceImpl().getPetStoreList();
            sendJson(exchange, 200, "{\"stores\":" + storesJson(stores) + "}");
        } catch (Exception ex) {
            sendServerError(exchange, ex);
        }
    }

    private void handleOwner(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) {
            return;
        }

        try {
            Map<String, String> query = queryParams(exchange.getRequestURI().getRawQuery());
            int ownerId = parseRequiredInt(query.get("id"), "缺少主人编号");
            PetOwner owner = loadOwner(ownerId);
            if (owner == null) {
                sendApiError(exchange, 404, "主人不存在");
                return;
            }

            PetStoreServiceImpl storeService = new PetStoreServiceImpl();
            sendJson(exchange, 200, "{\"owner\":" + ownerJson(owner) + ",\"pets\":" + petsJson(loadOwnerPets(ownerId), storeService) + "}");
        } catch (IllegalArgumentException ex) {
            sendApiError(exchange, 400, ex.getMessage());
        } catch (Exception ex) {
            sendServerError(exchange, ex);
        }
    }

    private void handleBuy(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) {
            return;
        }

        try {
            Map<String, String> request = parseJsonObject(readBody(exchange));
            int ownerId = parseRequiredInt(request.get("ownerId"), "缺少主人编号");
            int petId = parseRequiredInt(request.get("petId"), "缺少宠物编号");

            PetOwner owner = loadOwner(ownerId);
            if (owner == null) {
                sendApiError(exchange, 404, "主人不存在");
                return;
            }

            PetStoreServiceImpl storeService = new PetStoreServiceImpl();
            Pet pet = findStockPet(storeService, petId);
            if (pet == null) {
                sendApiError(exchange, 404, "该宠物已不在库存中");
                return;
            }

            int price = (int) Math.round(storeService.charge(pet));
            if (owner.getMoney() < price) {
                sendApiError(exchange, 409, "元宝不足，无法购买该宠物");
                return;
            }

            pet.setOwner_id(ownerId);
            new PetOwnerServiceImpl().buy(pet);

            PetOwner updatedOwner = loadOwner(ownerId);
            String response = "{"
                    + "\"message\":\"购买成功\","
                    + "\"price\":" + price + ","
                    + "\"owner\":" + ownerJson(updatedOwner) + ","
                    + "\"pets\":" + petsJson(loadOwnerPets(ownerId), storeService)
                    + "}";
            sendJson(exchange, 200, response);
        } catch (IllegalArgumentException ex) {
            sendApiError(exchange, 400, ex.getMessage());
        } catch (Exception ex) {
            sendServerError(exchange, ex);
        }
    }

    private Pet findStockPet(PetStoreServiceImpl storeService, int petId) throws Exception {
        List<Pet> pets = storeService.getPetsInstock(0);
        for (Pet pet : pets) {
            if (pet.getId() == petId) {
                return pet;
            }
        }
        return null;
    }

    private PetOwner loadOwner(int ownerId) throws Exception {
        PetOwnerDao ownerDao = new PetOwnerDaoImpl();
        return ownerDao.selectOwner("select id, name, password, money from petowner where id = ?", new Object[]{ownerId});
    }

    private List<Pet> loadOwnerPets(int ownerId) throws Exception {
        PetDao petDao = new PetDaoImpl();
        String sql = "select id, name, typename, health, love, birthday, owner_id, store_id from pet where owner_id = ? order by id";
        return petDao.selectPet(sql, new Object[]{ownerId});
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed", "text/plain; charset=utf-8");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        if ("/".equals(requestPath)) {
            requestPath = "/index.html";
        }

        String relativePath = URLDecoder.decode(requestPath.substring(1), StandardCharsets.UTF_8);
        Path target = webRoot.resolve(relativePath).normalize();
        if (!target.startsWith(webRoot) || Files.isDirectory(target) || !Files.exists(target)) {
            sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
            return;
        }

        byte[] content = Files.readAllBytes(target);
        exchange.getResponseHeaders().set("Content-Type", contentType(target));
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private boolean requireMethod(HttpExchange exchange, String expectedMethod) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return false;
        }
        if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
            sendApiError(exchange, 405, "请求方法不支持");
            return false;
        }
        return true;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseJsonObject(String body) {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = JSON_FIELD.matcher(body == null ? "" : body);
        while (matcher.find()) {
            String rawValue = matcher.group(3) == null ? matcher.group(2) : matcher.group(3);
            values.put(matcher.group(1), unescapeJson(rawValue));
        }
        return values;
    }

    private Map<String, String> queryParams(String query) {
        Map<String, String> values = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return values;
        }
        for (String pair : query.split("&")) {
            int index = pair.indexOf('=');
            String key = index >= 0 ? pair.substring(0, index) : pair;
            String value = index >= 0 ? pair.substring(index + 1) : "";
            values.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8)
            );
        }
        return values;
    }

    private int parseRequiredInt(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private int parseOptionalInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String petsJson(List<Pet> pets, PetStoreServiceImpl storeService) throws Exception {
        StringBuilder json = new StringBuilder("[");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < pets.size(); i++) {
            Pet pet = pets.get(i);
            if (i > 0) {
                json.append(',');
            }
            String birthday = pet.getBirthday() == null ? "" : dateFormat.format(pet.getBirthday());
            json.append('{')
                    .append("\"id\":").append(pet.getId()).append(',')
                    .append("\"name\":").append(jsonString(pet.getName())).append(',')
                    .append("\"type\":").append(jsonString(pet.getTypename())).append(',')
                    .append("\"health\":").append(pet.getHealth()).append(',')
                    .append("\"love\":").append(pet.getLove()).append(',')
                    .append("\"birthday\":").append(jsonString(birthday)).append(',')
                    .append("\"ownerId\":").append(pet.getOwner_id()).append(',')
                    .append("\"storeId\":").append(pet.getStore_id()).append(',')
                    .append("\"price\":").append((int) Math.round(storeService.charge(pet)))
                    .append('}');
        }
        return json.append(']').toString();
    }

    private String storesJson(List<PetStore> stores) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < stores.size(); i++) {
            PetStore store = stores.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                    .append("\"id\":").append(store.getId()).append(',')
                    .append("\"name\":").append(jsonString(store.getName())).append(',')
                    .append("\"balance\":").append(store.getBalance())
                    .append('}');
        }
        return json.append(']').toString();
    }

    private String ownerJson(PetOwner owner) {
        if (owner == null) {
            return "null";
        }
        return "{"
                + "\"id\":" + owner.getId() + ","
                + "\"name\":" + jsonString(owner.getName()) + ","
                + "\"money\":" + owner.getMoney()
                + "}";
    }

    private void sendServerError(HttpExchange exchange, Exception ex) throws IOException {
        logServerError(ex);
        sendApiError(exchange, 500, "服务器处理失败，请检查后端配置");
    }

    private void logServerError(Exception ex) {
        System.err.println("Server error: " + exceptionSummary(ex));
        Throwable cause = ex.getCause();
        while (cause != null) {
            System.err.println("Caused by: " + exceptionSummary(cause));
            cause = cause.getCause();
        }
    }

    private String exceptionSummary(Throwable ex) {
        StringBuilder summary = new StringBuilder(ex.getClass().getName());
        if (ex instanceof SQLException) {
            SQLException sqlException = (SQLException) ex;
            summary.append(" SQLState=").append(sqlException.getSQLState());
            summary.append(" ErrorCode=").append(sqlException.getErrorCode());
        }
        String message = sanitizeLogValue(ex.getMessage());
        if (!message.isEmpty()) {
            summary.append(" message=").append(message);
        }
        return summary.toString();
    }

    private String sanitizeLogValue(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?i)(password=)[^&\\s]+", "$1***")
                .replaceAll("(?i)(://[^:/\\s]+:)[^@\\s]+(@)", "$1***$2");
    }

    private void sendApiError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, "{\"message\":" + jsonString(message) + "}");
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        addCorsHeaders(exchange);
        byte[] content = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private void sendText(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        String allowedOrigin = allowedCorsOrigin(requestOrigin);
        if (allowedOrigin != null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String allowedCorsOrigin(String requestOrigin) {
        if ("*".equals(CORS_ORIGINS.trim())) {
            return "*";
        }
        if (requestOrigin == null || requestOrigin.trim().isEmpty()) {
            return null;
        }
        String[] origins = CORS_ORIGINS.split(",");
        for (String origin : origins) {
            if (requestOrigin.equals(origin.trim())) {
                return requestOrigin;
            }
        }
        return null;
    }

    private String contentType(Path target) {
        String fileName = target.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (fileName.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String unescapeJson(String value) {
        return value == null ? "" : value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        return result.append('"').toString();
    }

    private static String config(String propertyName, String envName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(envName);
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
