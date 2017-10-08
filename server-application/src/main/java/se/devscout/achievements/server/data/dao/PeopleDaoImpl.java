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
    public Person create(Organization parent, PersonProperties properties) {
        final Person person = new ModelMapper().map(properties, Person.class);
        person.setOrganization(parent);
        return persist(person);
    }

    @Override
    public Person update(Integer id, PersonProperties properties) throws ObjectNotFoundException {
        final Person person = read(id);
        person.apply(properties);
        return super.persist(person);
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
}
