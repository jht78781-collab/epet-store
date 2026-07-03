package cn.turing.manager;

import cn.turing.entity.Pet;

/**
 * 购买宠物的业务逻辑接口。
 */
public interface Buyable {
    void buy(Pet pet) throws Exception;
}
