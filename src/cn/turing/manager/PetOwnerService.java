package cn.turing.manager;

import cn.turing.entity.PetOwner;

/**
 * 主人业务逻辑接口。
 */
public interface PetOwnerService extends Buyable {
    PetOwner login(String ownerName, String ownerPass) throws Exception;
}
