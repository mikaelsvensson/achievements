package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.*;

import java.util.List;

public interface AchievementStepProgressDao {
    List<AchievementStepProgress> get(Achievement achievement);

    AchievementStepProgress get(AchievementStep achievementStep, Person person) throws ObjectNotFoundException;

    AchievementStepProgress set(AchievementStep achievementStep, Person person, AchievementStepProgressProperties properties);

    void unset(AchievementStep achievementStep, Person person) throws ObjectNotFoundException;
}
