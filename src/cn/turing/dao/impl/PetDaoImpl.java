package cn.turing.dao.impl;

import cn.turing.dao.BaseDao;
import cn.turing.dao.PetDao;
import cn.turing.entity.Pet;

import java.util.ArrayList;
import java.util.List;

/**
 * 宠物 DAO 实现类。
 */
public class PetDaoImpl extends BaseDao implements PetDao {
    @Override
    public List<Pet> selectPet(String sql, String[] param) throws Exception {
        List<Pet> pets = new ArrayList<>();
        try {
            openDB();
            psmt = conn.prepareStatement(sql);
            if (param != null) {
                for (int i = 0; i < param.length; i++) {
                    psmt.setString(i + 1, param[i]);
                }
            }
            rs = psmt.executeQuery();
            while (rs.next()) {
                Pet pet = new Pet();
                pet.setId(rs.getInt("id"));
                pet.setName(rs.getString("name"));
                pet.setTypename(rs.getString("typename"));
                pet.setHealth(rs.getInt("health"));
                pet.setLove(rs.getInt("love"));
                pet.setBirthday(rs.getDate("birthday"));
                pet.setOwner_id(rs.getInt("owner_id"));
                pet.setStore_id(rs.getInt("store_id"));
                pets.add(pet);
            }
        } finally {
            closeDB();
        }
        return pets;
    }

    @Override
    public int updatePet(String sql, Object[] param) throws Exception {
        return executeUpdate(sql, param);
    }
}
