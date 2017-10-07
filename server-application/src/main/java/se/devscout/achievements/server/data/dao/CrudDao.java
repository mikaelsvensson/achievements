package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Person;

import java.util.List;

public interface CrudDao<E, P, PE> extends BaseDao<E, P> {
    E create(PE parent, P properties);
    List<E> getByParent(PE parent);
}
