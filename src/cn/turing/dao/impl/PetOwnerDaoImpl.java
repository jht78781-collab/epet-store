package cn.turing.dao.impl;

import cn.turing.dao.BaseDao;
import cn.turing.dao.PetOwnerDao;
import cn.turing.entity.PetOwner;

import java.sql.PreparedStatement;

/**
 * 主人 DAO 实现类。
 */
public class PetOwnerDaoImpl extends BaseDao implements PetOwnerDao {
    @Override
    public PetOwner selectOwner(String sql, Object[] param) throws Exception {
        PetOwner owner = null;
        try {
            openDB();
            psmt = conn.prepareStatement(sql);
            if (param != null) {
                for (int i = 0; i < param.length; i++) {
                    psmt.setObject(i + 1, param[i]);
                }
            }
            rs = psmt.executeQuery();
            if (rs.next()) {
                owner = new PetOwner();
                owner.setId(rs.getInt("id"));
                owner.setName(rs.getString("name"));
                owner.setPassword(rs.getString("password"));
                owner.setMoney(rs.getInt("money"));
            }
        } finally {
            closeDB();
        }
        return owner;
    }

    @Override
    public int updateOwner(String sql, Object[] param) throws Exception {
        return executeUpdate(sql, param);
    }

    @Override
    public int callOwnerBuyProc(Object[] param) throws Exception {
        if (param == null || param.length < 4) {
            throw new IllegalArgumentException("主人购买宠物至少需要 ownerId、storeId、petId、dealCost 四个参数");
        }

        int ownerId = toInt(param[0]);
        int storeId = toInt(param[1]);
        int petId = toInt(param[2]);
        int dealCost = toInt(param[3]);
        int result = 0;

        try {
            openDB();
            conn.setAutoCommit(false);

            result += executeTransactionUpdate(
                    "update petowner set money = money - ? where id = ? and money >= ?",
                    dealCost, ownerId, dealCost
            );
            result += executeTransactionUpdate(
                    "update petstore set balance = balance + ? where id = ?",
                    dealCost, storeId
            );
            result += executeTransactionUpdate(
                    "update pet set owner_id = ? where id = ? and store_id = ? and owner_id is null",
                    ownerId, petId, storeId
            );
            result += executeTransactionUpdate(
                    "insert into account(deal_type, pet_id, seller_id, buyer_id, price, deal_time) values (?, ?, ?, ?, ?, current_timestamp)",
                    1, petId, storeId, ownerId, dealCost
            );

            if (result == 4) {
                conn.commit();
            } else {
                conn.rollback();
            }
        } catch (Exception ex) {
            if (conn != null) {
                conn.rollback();
            }
            throw ex;
        } finally {
            closeDB();
        }
        return result;
    }

    private int executeTransactionUpdate(String sql, Object... params) throws Exception {
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate();
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
