package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.net.URI;
import java.util.UUID;

public class AchievementStepDTO {
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
