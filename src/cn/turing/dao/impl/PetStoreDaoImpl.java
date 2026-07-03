package cn.turing.dao.impl;

import cn.turing.dao.BaseDao;
import cn.turing.dao.PetStoreDao;
import cn.turing.entity.PetStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 商店 DAO 实现类。
 */
public class
PetStoreDaoImpl extends BaseDao implements PetStoreDao {
    @Override
    public int updateStore(String sql, Object[] param) throws Exception {
        return executeUpdate(sql, param);
    }

    @Override
    public PetStore selectStore(String sql, Object[] param) throws Exception {
        PetStore store = null;
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
                store = mapStore();
            }
        } finally {
            closeDB();
        }
        return store;
    }

    @Override
    public List<PetStore> selectStroeList(String sql, Object[] param) throws Exception {
        List<PetStore> stores = new ArrayList<>();
        try {
            openDB();
            psmt = conn.prepareStatement(sql);
            if (param != null) {
                for (int i = 0; i < param.length; i++) {
                    psmt.setObject(i + 1, param[i]);
                }
            }
            rs = psmt.executeQuery();
            while (rs.next()) {
                stores.add(mapStore());
            }
        } finally {
            closeDB();
        }
        return stores;
    }

    private PetStore mapStore() throws Exception {
        PetStore store = new PetStore();
        store.setId(rs.getInt("id"));
        store.setName(rs.getString("name"));
        store.setPassword(rs.getString("password"));
        store.setBalance(rs.getInt("balance"));
        return store;
    }
}
