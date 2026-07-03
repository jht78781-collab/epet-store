package cn.turing.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 宠物实体类，对应数据库 pet 表。
 */
public class Pet {
    private int id;
    private String name;
    private String typename;
    private int health;
    private int love;
    private Date birthday;
    private int owner_id;
    private int store_id;

    public Pet() {
    }

    public Pet(int id, String name, String typename, int health, int love, Date birthday, int owner_id, int store_id) {
        this.id = id;
        this.name = name;
        this.typename = typename;
        this.health = health;
        this.love = love;
        this.birthday = birthday;
        this.owner_id = owner_id;
        this.store_id = store_id;
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

    public String getTypename() {
        return typename;
    }

    public void setTypename(String typename) {
        this.typename = typename;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getLove() {
        return love;
    }

    public void setLove(int love) {
        this.love = love;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public int getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(int owner_id) {
        this.owner_id = owner_id;
    }

    public int getStore_id() {
        return store_id;
    }

    public void setStore_id(int store_id) {
        this.store_id = store_id;
    }

    @Override
    public String toString() {
        String dateText = birthday == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(birthday);
        return "Pet{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", typename='" + typename + '\'' +
                ", health=" + health +
                ", love=" + love +
                ", birthday=" + dateText +
                ", owner_id=" + owner_id +
                ", store_id=" + store_id +
                '}';
    }
}
