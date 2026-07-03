package cn.turing.dao;

import cn.turing.entity.Pet;

import java.util.List;

/**
 * 宠物 DAO 接口。
 */
public interface PetDao {
    List<Pet> selectPet(String sql, String[] param) throws Exception;

    int updatePet(String sql, Object[] param) throws Exception;
}
