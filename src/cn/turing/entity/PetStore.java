package cn.turing.entity;

/**
 * 宠物商店实体类，对应数据库 petstore 表。
 */
public class PetStore {
    private int id;
    private String name;
    private String password;
    private int balance;

    public PetStore() {
    }

    public PetStore(int id, String name, String password, int balance) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.balance = balance;
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

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "PetStore{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", balance=" + balance +
                '}';
    }
}
