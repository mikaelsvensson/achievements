package se.devscout.achievements.server.data.dao;

public interface BaseDao<E, P> {
    E get(String id) throws ObjectNotFoundException;

    E update(String id, P properties) throws ObjectNotFoundException;

    void delete(String id) throws ObjectNotFoundException;
}
