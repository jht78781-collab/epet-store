package cn.turing.manager.impl;

import cn.turing.dao.PetDao;
import cn.turing.dao.PetStoreDao;
import cn.turing.dao.impl.PetDaoImpl;
import cn.turing.dao.impl.PetStoreDaoImpl;
import cn.turing.entity.Pet;
import cn.turing.entity.PetStore;
import cn.turing.manager.PetStoreService;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 商店业务逻辑实现类。
 */
public class PetStoreServiceImpl implements PetStoreService {
    private final PetDao petDao = new PetDaoImpl();
    private final PetStoreDao petStoreDao = new PetStoreDaoImpl();

    @Override
    public double charge(Pet pet) throws Exception {
        int age = getPetAge(pet.getBirthday());
        return age <= 3 ? 5 : 3;
    }

    @Override
    public List<Pet> getPetsInstock(int storeId) throws Exception {
        String sql = "select id, name, typename, health, love, birthday, owner_id, store_id from pet where owner_id is null";
        String[] param = null;
        if (storeId != 0) {
            sql += " and store_id = ?";
            param = new String[]{String.valueOf(storeId)};
        }
        sql += " order by id";
        return petDao.selectPet(sql, param);
    }

    @Override
    public List<PetStore> getPetStoreList() throws Exception {
        String sql = "select id, name, password, balance from petstore order by id";
        return petStoreDao.selectStroeList(sql, null);
    }

    @Override
    public void buy(Pet pet) throws Exception {
        throw new UnsupportedOperationException("商店购买主人宠物功能尚未开通");
    }

    private int getPetAge(Date birthday) {
        if (birthday == null) {
            return Integer.MAX_VALUE;
        }
        LocalDate birthDate;
        if (birthday instanceof java.sql.Date) {
            birthDate = ((java.sql.Date) birthday).toLocalDate();
        } else {
            birthDate = birthday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
