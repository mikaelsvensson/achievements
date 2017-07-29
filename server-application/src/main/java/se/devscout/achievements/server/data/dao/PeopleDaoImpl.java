package se.devscout.achievements.server.data.dao;

import org.hibernate.SessionFactory;
import org.modelmapper.ModelMapper;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;

import java.util.List;
import java.util.UUID;

public class PeopleDaoImpl extends DaoImpl<Person> implements PeopleDao {
    public PeopleDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Person get(String id) throws ObjectNotFoundException {
        return getEntity(Integer.valueOf(id));
    }

    @Override
    public Person create(Organization parent, PersonProperties properties) {
        final Person person = new ModelMapper().map(properties, Person.class);
        person.setOrganization(parent);
        return persist(person);
    }

    @Override
    public Person update(String id, PersonProperties properties) throws ObjectNotFoundException {
        final Person person = get(id);
        person.apply(properties);
        return super.persist(person);
    }

    @Override
    public void delete(String id) throws ObjectNotFoundException {
        final Person person = get(id);
        super.currentSession().delete(person);
    }

    @Override
    public List<Person> getByOrganization(String organizationId) {
        return namedQuery("Person.getByOrganization")
                .setParameter("organizationId", UUID.fromString(organizationId))
                .getResultList();
    }
}
