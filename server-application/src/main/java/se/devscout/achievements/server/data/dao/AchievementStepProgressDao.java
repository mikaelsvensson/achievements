package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProgress;
import se.devscout.achievements.server.data.model.AchievementStepProgressProperties;
import se.devscout.achievements.server.data.model.Person;

import java.util.List;

public interface AchievementStepProgressDao {
    AchievementStepProgress get(AchievementStep achievementStep, Person person) throws ObjectNotFoundException;

    AchievementStepProgress set(AchievementStep achievementStep, Person person, AchievementStepProgressProperties properties) throws ObjectNotFoundException;

    void unset(AchievementStep achievementStep, Person person) throws ObjectNotFoundException;
}
