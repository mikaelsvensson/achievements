package se.devscout.achievements.server.data.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.io.Serializable;

public class DaoImpl<E, ID extends Serializable> extends AbstractDAO<E> {
    public DaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    protected E getEntity(ID id) throws ObjectNotFoundException {
        final E organization = get(id);
        if (organization != null) {
            return organization;
        } else {
            throw new ObjectNotFoundException();
        }
    }
}
