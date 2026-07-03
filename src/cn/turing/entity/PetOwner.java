package cn.turing.entity;

/**
 * 宠物主人实体类，对应数据库 petowner 表。
 */
public class PetOwner {
    private int id;
    private String name;
    private String password;
    private int money;

    public PetOwner() {
    }

    public PetOwner(int id, String name, String password, int money) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.money = money;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    @Override
    public String toString() {
        return "PetOwner{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", money=" + money +
                '}';
    }
}
