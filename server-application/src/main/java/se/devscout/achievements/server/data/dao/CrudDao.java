package se.devscout.achievements.server.data.dao;

public interface CrudDao<E, P, PE> extends BaseDao<E, P> {
    E create(PE parent, P properties);
}
