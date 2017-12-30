package se.devscout.achievements.server.data.dao;

import org.hibernate.SessionFactory;
import org.modelmapper.ModelMapper;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsProperties;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.data.model.Person;

import java.util.List;
import java.util.UUID;

public class CredentialsDaoImpl extends DaoImpl<Credentials, UUID> implements CredentialsDao {
    public CredentialsDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Credentials get(CredentialsType provider, String username) throws ObjectNotFoundException {
        final List list = namedQuery("Credentials.getByUsername")
                .setParameter("type", provider)
                .setParameter("userId", username)
                .getResultList();
        if (list.size() == 1) {
            return (Credentials) list.get(0);
        } else {
            throw new ObjectNotFoundException();
        }
    }

    @Override
    public Credentials read(UUID uuid) throws ObjectNotFoundException {
        return getEntity(uuid);
    }

    @Override
    public List<Credentials> readAll() {
        return readAll(Credentials.class);
    }

    @Override
    public Credentials update(UUID uuid, CredentialsProperties properties) throws ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(UUID uuid) throws ObjectNotFoundException {
        throw new UnsupportedOperationException();

    }

    @Override
    public Credentials create(Person parent, CredentialsProperties properties) {
        final Credentials person = new ModelMapper().map(properties, Credentials.class);
        person.setPerson(parent);
        return persist(person);
    }

    @Override
    public List<Credentials> getByParent(Person parent) {
        return namedQuery("Credentials.getByPerson")
                .setParameter("person", parent)
                .getResultList();
    }
}
