package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;

import java.util.List;

public interface PeopleDao extends CrudDao<Person, PersonProperties, Organization, Integer> {
    Person read(Organization parent, String customId) throws ObjectNotFoundException;

    List<Person> getByEmail(String email);

    List<Person> getByAwardedAchievement(Organization organization, Achievement achievement);

    void addAwardFor(Person person, Achievement achievement);

    void removeAwardFor(Person person, Achievement achievement);
}
