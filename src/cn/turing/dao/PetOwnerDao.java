package cn.turing.dao;

import cn.turing.entity.PetOwner;

/**
 * 主人 DAO 接口。
 */
public interface PetOwnerDao {
    PetOwner selectOwner(String sql, Object[] param) throws Exception;

    int updateOwner(String sql, Object[] param) throws Exception;

    int callOwnerBuyProc(Object[] param) throws Exception;
}
