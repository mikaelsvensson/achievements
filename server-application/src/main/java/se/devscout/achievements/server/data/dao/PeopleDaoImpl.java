package se.devscout.achievements.server.data.dao;

import org.hibernate.SessionFactory;
import org.modelmapper.ModelMapper;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;

import java.util.List;

public class PeopleDaoImpl extends DaoImpl<Person, Integer> implements PeopleDao {
    public PeopleDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Person read(Integer id) throws ObjectNotFoundException {
        try {
            return getEntity(id);
        } catch (NumberFormatException e) {
            //TODO: Log this. Could be simple client error or hacking attempt.
            throw new ObjectNotFoundException();
        }
    }

    @Override
    public List<Person> readAll() {
        return readAll(Person.class);
    }

    @Override
    public Person create(Organization parent, PersonProperties properties) throws DuplicateCustomIdentifier {
        verifyCustomIdentifier(parent, properties);
        final Person person = new ModelMapper().map(properties, Person.class);
        person.setOrganization(parent);
        return persist(person);
    }

    @Override
    public Person update(Integer id, PersonProperties properties) throws ObjectNotFoundException, DuplicateCustomIdentifier {
        final Person person = read(id);
        verifyCustomIdentifier(person.getOrganization(), properties);
        person.apply(properties);
        return super.persist(person);
    }

    private void verifyCustomIdentifier(Organization parent, PersonProperties properties) throws DuplicateCustomIdentifier {
        if (isExistingCustomId(parent, properties.getCustomIdentifier())) {
            throw new DuplicateCustomIdentifier("Another person withing " + parent.getName() + " already has the identifier " + properties.getCustomIdentifier());
        }
    }

    @Override
    public void delete(Integer id) throws ObjectNotFoundException {
        final Person person = read(id);
        super.currentSession().delete(person);
    }

    @Override
    public List<Person> getByParent(Organization parent) {
        return namedQuery("Person.getByOrganization")
                .setParameter("organization", parent)
                .getResultList();
    }

    boolean isExistingCustomId(Organization parent, String customIdentifier) {
        return !findByCustomId(parent, customIdentifier).isEmpty();
    }

    private List<Person> findByCustomId(Organization parent, String customIdentifier) {
        return namedQuery("Person.getByCustomId")
                .setParameter("organization", parent)
                .setParameter("customId", customIdentifier)
                .getResultList();
    }

    @Override
    public List<Person> getByEmail(String email) {
        return namedQuery("Person.getByEmail")
                .setParameter("email", email)
                .getResultList();
    }

    @Override
    public Person read(Organization parent, String customId) throws ObjectNotFoundException {
        final List<Person> list = findByCustomId(parent, customId);
        if (list.isEmpty()) {
            throw new ObjectNotFoundException();
        }
        return list.iterator().next();
    }
}
