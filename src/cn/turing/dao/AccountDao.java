package cn.turing.dao;

/**
 * 台账 DAO 接口。
 */
public interface AccountDao {
    int updateAccount(String sql, Object[] param) throws Exception;
}
