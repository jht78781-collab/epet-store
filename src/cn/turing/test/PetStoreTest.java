package cn.turing.test;

import cn.turing.entity.Pet;
import cn.turing.entity.PetOwner;
import cn.turing.manager.impl.PetOwnerServiceImpl;
import cn.turing.manager.impl.PetStoreServiceImpl;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Scanner;

/**
 * 宠物商店控制台测试类。
 */
public class PetStoreTest {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final PetOwnerServiceImpl OWNER_SERVICE = new PetOwnerServiceImpl();
    private static final PetStoreServiceImpl STORE_SERVICE = new PetStoreServiceImpl();
    private static final int[] PET_TABLE_WIDTHS = {6, 10, 14, 10, 8, 8, 12, 10, 6};

    public static void main(String[] args) {
        try {
            System.out.println("欢迎来到宠物商店！");
            showAllStockPets();
            PetOwner owner = ownerLogin();
            showOwnerMenu(owner);
        } catch (Exception e) {
            System.out.println("系统异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static PetOwner ownerLogin() throws Exception {
        while (true) {
            System.out.println();
            System.out.println("请先登录宠物主人账号");
            System.out.print("请输入主人姓名：");
            String ownerName = SCANNER.nextLine().trim();
            System.out.print("请输入主人密码：");
            String ownerPass = SCANNER.nextLine().trim();

            PetOwner owner = OWNER_SERVICE.login(ownerName, ownerPass);
            if (owner == null) {
                System.out.println("登录失败，主人姓名或密码不正确，请重新登录。");
            } else {
                System.out.println("登录成功！");
                System.out.println("主人编号：" + owner.getId() + "，主人姓名：" + owner.getName() + "，当前元宝：" + owner.getMoney());
                return owner;
            }
        }
    }

    private static void showOwnerMenu(PetOwner owner) throws Exception {
        while (true) {
            System.out.println();
            System.out.println("请选择要办理的业务：");
            System.out.println("1. 购买宠物");
            System.out.println("2. 卖出宠物");
            System.out.println("0. 退出系统");
            int choice = readInt("请选择：", 0, 2);

            if (choice == 1) {
                ownerBuy(owner);
            } else if (choice == 2) {
                System.out.println("该功能尚未开通！抱歉。");
            } else {
                System.out.println("谢谢使用，再见！");
                return;
            }
        }
    }

    public static void ownerBuy(PetOwner owner) throws Exception {
        System.out.println();
        System.out.println("请选择购买方式：");
        System.out.println("1. 购买库存宠物");
        System.out.println("2. 购买新培育宠物");
        int buyType = readInt("请选择：", 1, 2);

        if (buyType == 2) {
            System.out.println("该功能尚未开通！抱歉。");
            return;
        }

        List<Pet> pets = STORE_SERVICE.getPetsInstock(0);
        if (pets.isEmpty()) {
            System.out.println("当前没有可购买的库存宠物。");
            return;
        }

        displayPets(pets);
        int choice = readInt("请选择要购买的宠物序号：", 1, pets.size());
        Pet pet = pets.get(choice - 1);
        int price = (int) Math.round(STORE_SERVICE.charge(pet));
        if (owner.getMoney() < price) {
            System.out.println("购买失败，当前元宝不足。该宠物价格为 " + price + " 元宝。");
            return;
        }

        pet.setOwner_id(owner.getId());
        OWNER_SERVICE.buy(pet);
        owner.setMoney(owner.getMoney() - price);
        System.out.println("购买成功！你购买了 " + pet.getName() + "，花费 " + price + " 元宝，剩余 " + owner.getMoney() + " 元宝。");
    }

    private static void showAllStockPets() throws Exception {
        List<Pet> pets = STORE_SERVICE.getPetsInstock(0);
        if (pets.isEmpty()) {
            System.out.println("当前没有库存宠物。");
            return;
        }
        System.out.println();
        System.out.println("当前库存宠物如下：");
        displayPets(pets);
    }

    private static void displayPets(List<Pet> pets) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        printPetRow("序号", "宠物编号", "宠物名称", "类型", "健康值", "亲密度", "生日", "商店编号", "价格");
        printPetSeparator();
        for (int i = 0; i < pets.size(); i++) {
            Pet pet = pets.get(i);
            String birthday = pet.getBirthday() == null ? "" : format.format(pet.getBirthday());
            int price = (int) Math.round(STORE_SERVICE.charge(pet));
            printPetRow(
                    String.valueOf(i + 1),
                    String.valueOf(pet.getId()),
                    pet.getName(),
                    pet.getTypename(),
                    String.valueOf(pet.getHealth()),
                    String.valueOf(pet.getLove()),
                    birthday,
                    String.valueOf(pet.getStore_id()),
                    String.valueOf(price)
            );
        }
    }

    private static void printPetRow(String... values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            line.append(padRight(values[i], PET_TABLE_WIDTHS[i]));
            if (i < values.length - 1) {
                line.append("  ");
            }
        }
        System.out.println(line);
    }

    private static void printPetSeparator() {
        int totalWidth = 0;
        for (int width : PET_TABLE_WIDTHS) {
            totalWidth += width;
        }
        totalWidth += (PET_TABLE_WIDTHS.length - 1) * 2;
        System.out.println("-".repeat(totalWidth));
    }

    private static String padRight(String value, int width) {
        String text = value == null ? "" : value;
        StringBuilder result = new StringBuilder(text);
        int spaces = width - displayWidth(text);
        for (int i = 0; i < spaces; i++) {
            result.append(' ');
        }
        return result.toString();
    }

    private static int displayWidth(String value) {
        int width = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            width += isWideCodePoint(codePoint) ? 2 : 1;
            i += Character.charCount(codePoint);
        }
        return width;
    }

    private static boolean isWideCodePoint(int codePoint) {
        return (codePoint >= 0x1100 && codePoint <= 0x115F)
                || (codePoint >= 0x2E80 && codePoint <= 0xA4CF)
                || (codePoint >= 0xAC00 && codePoint <= 0xD7A3)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0xFE10 && codePoint <= 0xFE19)
                || (codePoint >= 0xFE30 && codePoint <= 0xFE6F)
                || (codePoint >= 0xFF00 && codePoint <= 0xFF60)
                || (codePoint >= 0xFFE0 && codePoint <= 0xFFE6);
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = SCANNER.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("输入有误，请输入 " + min + " 到 " + max + " 之间的数字。");
        }
    }
}
