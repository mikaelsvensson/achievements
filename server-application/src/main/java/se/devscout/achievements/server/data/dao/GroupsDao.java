package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Group;
import se.devscout.achievements.server.data.model.GroupProperties;
import se.devscout.achievements.server.data.model.Organization;

public interface GroupsDao extends CrudDao<Group, GroupProperties, Organization, Integer> {
    Group read(Organization organization, String name) throws ObjectNotFoundException;
}
