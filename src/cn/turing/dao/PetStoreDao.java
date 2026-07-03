package cn.turing.dao;

import cn.turing.entity.PetStore;

import java.util.List;

/**
 * 商店 DAO 接口。
 */
public interface PetStoreDao {
    int updateStore(String sql, Object[] param) throws Exception;

    PetStore selectStore(String sql, Object[] param) throws Exception;

    List<PetStore> selectStroeList(String sql, Object[] param) throws Exception;
}
