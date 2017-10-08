package se.devscout.achievements.server.api;

import java.util.UUID;

public class AchievementStepDTO {
    public Integer id;

    public UUID prerequisite_achievement;

    public String description;

    public AchievementStepDTO() {
    }

    public AchievementStepDTO(UUID prerequisite_achievement) {
        this.prerequisite_achievement = prerequisite_achievement;
    }

    public AchievementStepDTO(String description) {
        this.description = description;
    }
}
