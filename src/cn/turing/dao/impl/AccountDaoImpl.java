package cn.turing.dao.impl;

import cn.turing.dao.AccountDao;
import cn.turing.dao.BaseDao;

/**
 * 台账 DAO 实现类。
 */
public class AccountDaoImpl extends BaseDao implements AccountDao {
    @Override
    public int updateAccount(String sql, Object[] param) throws Exception {
        return executeUpdate(sql, param);
    }
}
