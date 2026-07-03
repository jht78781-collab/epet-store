package cn.turing.manager.impl;

import cn.turing.dao.PetOwnerDao;
import cn.turing.dao.impl.PetOwnerDaoImpl;
import cn.turing.entity.Pet;
import cn.turing.entity.PetOwner;
import cn.turing.manager.PetOwnerService;

/**
 * 主人业务逻辑实现类。
 */
public class PetOwnerServiceImpl implements PetOwnerService {
    private final PetOwnerDao petOwnerDao = new PetOwnerDaoImpl();
    private final PetStoreServiceImpl petStoreService = new PetStoreServiceImpl();

    @Override
    public PetOwner login(String ownerName, String ownerPass) throws Exception {
        String sql = "select id, name, password, money from petowner where name = ? and password = ?";
        Object[] param = {ownerName, ownerPass};
        return petOwnerDao.selectOwner(sql, param);
    }

    @Override
    public void buy(Pet pet) throws Exception {
        if (pet == null) {
            throw new IllegalArgumentException("待购买宠物不能为空");
        }
        if (pet.getOwner_id() <= 0) {
            throw new IllegalArgumentException("购买前请先设置宠物主人编号");
        }
        if (pet.getStore_id() <= 0) {
            throw new IllegalArgumentException("宠物所属商店编号不正确");
        }

        int dealCost = (int) Math.round(petStoreService.charge(pet));
        Object[] param = {pet.getOwner_id(), pet.getStore_id(), pet.getId(), dealCost};
        int result = petOwnerDao.callOwnerBuyProc(param);
        if (result != 4) {
            throw new RuntimeException("购买失败，请确认宠物仍在库存中且主人余额足够");
        }
    }
}
