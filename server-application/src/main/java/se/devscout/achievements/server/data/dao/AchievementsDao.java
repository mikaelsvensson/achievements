package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;
import se.devscout.achievements.server.data.model.Organization;

import java.util.List;

public interface AchievementsDao extends CrudRootDao<Achievement, AchievementProperties> {
    List<Achievement> find(String name);
}
