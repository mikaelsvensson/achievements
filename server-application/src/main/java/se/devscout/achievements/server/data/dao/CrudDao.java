package se.devscout.achievements.server.data.dao;

import java.io.Serializable;
import java.util.List;

public interface CrudDao<E, P, PE, ID extends Serializable> extends BaseDao<E, P, ID> {
    E create(PE parent, P properties) throws DaoException;
    List<E> getByParent(PE parent);
}
