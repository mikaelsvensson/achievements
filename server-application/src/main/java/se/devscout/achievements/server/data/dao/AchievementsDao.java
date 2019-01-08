package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;

import java.util.List;
import java.util.UUID;

public interface AchievementsDao extends CrudRootDao<Achievement, AchievementProperties, UUID> {
    List<Achievement> find(String name);

    List<Achievement> findWithProgressForOrganization(Organization organization);

    List<Achievement> findWithProgressForPerson(Person person);

    void addAwardedTo(Achievement achievement, Person person);

    void removeAwardedTo(Achievement achievement, Person person);
}
