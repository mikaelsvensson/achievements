package se.devscout.achievements.server.data.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.model.*;

import java.util.List;

public class GroupMembershipsDaoImpl extends AbstractDAO<GroupMembership> implements GroupMembershipsDao {

    public GroupMembershipsDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public void add(Person person, Group group, GroupRole role) {
        final GroupMembership existing = get(new GroupMembershipId(group, person));
        if (existing == null) {
            final GroupMembership membership = new GroupMembership();
            membership.setGroup(group);
            membership.setPerson(person);
            membership.setRole(role);
            persist(membership);
        } else {
            // TODO: Use the .apply(...) pattern instead? Should add(...) actually update existing memberships?
            existing.setRole(role);
            persist(existing);
        }
    }

    @Override
    public void remove(Person person, Group group) {
        final GroupMembership membership = get(new GroupMembershipId(group, person));
        if (membership != null) {
            currentSession().delete(membership);
        } else {
            // TODO: Throw exception if nothing was changed?
        }
    }

    @Override
    public List<GroupMembership> getMemberships(Group group) {
        return namedQuery("GroupMembership.getByGroup")
                .setParameter("grp", group)
                .getResultList();
    }
}
