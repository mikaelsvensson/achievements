package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;
import se.devscout.achievements.server.data.model.Organization;

import java.util.List;
import java.util.UUID;

public interface AchievementsDao extends CrudRootDao<Achievement, AchievementProperties, UUID> {
    List<Achievement> find(String name);

    List<Achievement> findWithProgressForOrganization(Organization organization);
}
