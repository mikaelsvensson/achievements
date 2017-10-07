package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

public class AchievementDTO {
    public String id;
    public String name;
    public String description;
    public List<String> tags;
    public URI image;
    public List<AchievementStepDTO> steps;

    public AchievementDTO() {
    }

    public AchievementDTO(@JsonProperty("name") String name, @JsonProperty("steps") List<AchievementStepDTO> steps) {
        this.name = name;
        this.steps = steps;
    }
}
