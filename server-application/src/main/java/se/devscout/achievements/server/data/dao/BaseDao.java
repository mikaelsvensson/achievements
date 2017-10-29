package se.devscout.achievements.server.data.dao;

import java.io.Serializable;
import java.util.List;

public interface BaseDao<E, P, ID extends Serializable> {
    E read(ID id) throws ObjectNotFoundException;

    List<E> readAll();

    E update(ID id, P properties) throws ObjectNotFoundException;

    void delete(ID id) throws ObjectNotFoundException;
}
