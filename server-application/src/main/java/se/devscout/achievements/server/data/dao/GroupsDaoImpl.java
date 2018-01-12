package se.devscout.achievements.server.data.dao;

import org.hibernate.SessionFactory;
import org.modelmapper.ModelMapper;
import se.devscout.achievements.server.data.model.Group;
import se.devscout.achievements.server.data.model.GroupProperties;
import se.devscout.achievements.server.data.model.Organization;

import java.util.List;

public class GroupsDaoImpl extends DaoImpl<Group, Integer> implements GroupsDao {
    public GroupsDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Group read(Integer id) throws ObjectNotFoundException {
        return getEntity(id);
    }

    @Override
    public List<Group> readAll() {
        return readAll(Group.class);
    }

    @Override
    public Group create(Organization parent, GroupProperties properties) throws DaoException {
        verifyGroupName(parent, properties.getName());
        final Group group = new ModelMapper().map(properties, Group.class);
        group.setOrganization(parent);
        return persist(group);
    }

    @Override
    public Group update(Integer id, GroupProperties properties) throws ObjectNotFoundException, DaoException {
        final Group group = read(id);
        verifyGroupName(group.getOrganization(), properties.getName());
        group.apply(properties);
        return super.persist(group);
    }

    private void verifyGroupName(Organization parent, String name) throws DuplicateCustomIdentifier {
        if (!findByName(parent, name).isEmpty()) {
            throw new DuplicateCustomIdentifier("Another group within " + parent.getName() + " is already named " + name);
        }
    }

    @Override
    public void delete(Integer id) throws ObjectNotFoundException {
        final Group group = read(id);
        super.currentSession().delete(group);
    }

    @Override
    public List<Group> getByParent(Organization parent) {
        return namedQuery("Group.getByOrganization")
                .setParameter("organization", parent)
                .getResultList();
    }

    boolean isExistingCustomId(Organization parent, String groupName) {
        return !findByName(parent, groupName).isEmpty();
    }

    private List<Group> findByName(Organization parent, String groupName) {
        return namedQuery("Group.getByName")
                .setParameter("organization", parent)
                .setParameter("name", groupName)
                .getResultList();
    }

}
