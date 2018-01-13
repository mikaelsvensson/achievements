package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Group;
import se.devscout.achievements.server.data.model.GroupMembership;
import se.devscout.achievements.server.data.model.GroupRole;
import se.devscout.achievements.server.data.model.Person;

import java.util.List;

public interface GroupMembershipsDao {
    void add(Person person, Group group, GroupRole role);

    void remove(Person person, Group group);

    List<GroupMembership> getMemberships(Group group);
}
