package cn.turing.manager;

import cn.turing.entity.Pet;

import java.util.List;

/**
 * 商店业务逻辑接口。
 */
public interface PetStoreService extends Buyable {
    double charge(Pet pet) throws Exception;

    List getPetsInstock(int storeId) throws Exception;

    List getPetStoreList() throws Exception;
}
