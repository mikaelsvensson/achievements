package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class AchievementDTO extends AchievementBaseDTO {
    public String description;
    public List<AchievementStepDTO> steps = new ArrayList<>();

    public AchievementDTO() {
    }

    public AchievementDTO(@JsonProperty("name") String name,
                          @JsonProperty("steps") List<AchievementStepDTO> steps) {
        this.name = name;
        this.steps = steps;
    }
}
