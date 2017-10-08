package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;

import java.util.List;
import java.util.UUID;

public interface AchievementsDao extends CrudRootDao<Achievement, AchievementProperties, UUID> {
    List<Achievement> find(String name);
}
