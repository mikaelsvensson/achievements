package se.devscout.achievements.server.data.dao;

public interface CrudRootDao<E, P> extends BaseDao<E, P> {
    E create(P properties) throws DaoException;
}
