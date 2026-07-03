package cn.turing.dao;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DAO 工具类，负责打开数据库连接、关闭资源和执行通用更新操作。
 */
public class BaseDao {
    private static final Map<String, String> DOTENV = loadDotEnv();
    private static final String RAW_URL = config(
            "epet.db.url",
            new String[]{"EPET_DB_URL", "DATABASE_URL"},
            "jdbc:mysql://localhost:3306/epet?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
    );
    private static final boolean HAS_URL_CREDENTIALS = hasUrlCredentials(RAW_URL);
    private static final boolean HAS_EXPLICIT_USER = hasConfig("epet.db.user", "EPET_DB_USER");

    public static final String URL = normalizeJdbcUrl(RAW_URL);
    public static final String DRIVER = config("epet.db.driver", new String[]{"EPET_DB_DRIVER"}, defaultDriver(URL));
    public static final String DBNAME = config("epet.db.user", "EPET_DB_USER", "root");
    public static final String DBPASS = config("epet.db.password", "EPET_DB_PASSWORD", "root");

    public Connection conn;
    public PreparedStatement psmt;
    public ResultSet rs;

    private static String config(String propertyName, String envName, String defaultValue) {
        return config(propertyName, new String[]{envName}, defaultValue);
    }

    private static String config(String propertyName, String[] envNames, String defaultValue) {
        String value = System.getProperty(propertyName);
        for (String envName : envNames) {
            if (value == null || value.trim().isEmpty()) {
                value = System.getenv(envName);
            }
            if (value == null || value.trim().isEmpty()) {
                value = DOTENV.get(envName);
            }
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static Map<String, String> loadDotEnv() {
        Path dotEnvPath = Path.of(".env").toAbsolutePath().normalize();
        if (!Files.isRegularFile(dotEnvPath)) {
            return Map.of();
        }

        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(dotEnvPath, StandardCharsets.UTF_8)) {
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring("export ".length()).trim();
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2) {
                    char first = value.charAt(0);
                    char last = value.charAt(value.length() - 1);
                    if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                        value = value.substring(1, value.length() - 1);
                    }
                }
                values.put(key, value);
            }
        } catch (Exception ex) {
            return Map.of();
        }
        return values;
    }

    private static boolean hasConfig(String propertyName, String envName) {
        String property = System.getProperty(propertyName);
        String env = System.getenv(envName);
        return (property != null && !property.trim().isEmpty()) || (env != null && !env.trim().isEmpty());
    }

    private static String defaultDriver(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        return "com.mysql.cj.jdbc.Driver";
    }

    private static boolean hasUrlCredentials(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl.replaceFirst("^jdbc:", ""));
            if (uri.getUserInfo() != null && !uri.getUserInfo().trim().isEmpty()) {
                return true;
            }
            String query = uri.getRawQuery();
            return query != null && query.toLowerCase().matches(".*(^|&)(user|password)=.*");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String normalizeJdbcUrl(String rawUrl) {
        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            return normalizePostgresUrl(rawUrl);
        }
        if (rawUrl.startsWith("mysql://")) {
            return "jdbc:" + rawUrl;
        }
        return rawUrl;
    }

    private static String normalizePostgresUrl(String rawUrl) {
        URI uri = URI.create(rawUrl);
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null && !uri.getRawPath().isEmpty()) {
            jdbcUrl.append(uri.getRawPath());
        }

        String separator = uri.getRawQuery() == null || uri.getRawQuery().isEmpty() ? "?" : "?" + uri.getRawQuery() + "&";
        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            String[] parts = userInfo.split(":", 2);
            jdbcUrl.append(separator)
                    .append("user=").append(urlEncode(parts[0]));
            if (parts.length > 1) {
                jdbcUrl.append("&password=").append(urlEncode(parts[1]));
            }
            if (!jdbcUrl.toString().contains("sslmode=")) {
                jdbcUrl.append("&sslmode=require");
            }
        } else if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }
        return jdbcUrl.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public void openDB() throws Exception {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException ex) {
            if ("com.mysql.cj.jdbc.Driver".equals(DRIVER)) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                } catch (ClassNotFoundException legacyEx) {
                    ClassNotFoundException missingDriver = new ClassNotFoundException(
                            "MySQL JDBC driver not found. Add lib/mysql-connector-j-8.4.0.jar to the classpath.",
                            ex
                    );
                    missingDriver.addSuppressed(legacyEx);
                    throw missingDriver;
                }
            } else {
                throw new ClassNotFoundException("JDBC driver not found: " + DRIVER, ex);
            }
        }

        if (HAS_URL_CREDENTIALS && !HAS_EXPLICIT_USER) {
            conn = DriverManager.getConnection(URL);
        } else {
            conn = DriverManager.getConnection(URL, DBNAME, DBPASS);
        }
    }

    public void closeDB() throws Exception {
        if (rs != null) {
            rs.close();
            rs = null;
        }
        if (psmt != null) {
            psmt.close();
            psmt = null;
        }
        if (conn != null) {
            conn.close();
            conn = null;
        }
    }

    public int executeUpdate(String sql, Object[] objects) throws Exception {
        int result;
        try {
            openDB();
            psmt = conn.prepareStatement(sql);
            if (objects != null) {
                for (int i = 0; i < objects.length; i++) {
                    psmt.setObject(i + 1, objects[i]);
                }
            }
            result = psmt.executeUpdate();
        } finally {
            closeDB();
        }
        return result;
    }
}
