package se.devscout.achievements.server.data.dao;

import java.io.Serializable;

public interface CrudRootDao<E, P, ID extends Serializable> extends BaseDao<E, P, ID> {
    E create(P properties) throws DaoException;
}
