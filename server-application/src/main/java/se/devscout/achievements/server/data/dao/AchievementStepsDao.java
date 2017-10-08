package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProperties;

public interface AchievementStepsDao extends CrudDao<AchievementStep, AchievementStepProperties, Achievement, Integer> {
}
