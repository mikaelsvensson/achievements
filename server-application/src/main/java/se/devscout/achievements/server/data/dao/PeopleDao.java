package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;

public interface PeopleDao extends CrudDao<Person, PersonProperties, Organization> {
}
