package se.devscout.achievements.server.data.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("standard")
public class AchievementStandardStep extends AchievementStep {

    private String description;

    public AchievementStandardStep() {
    }

    public AchievementStandardStep(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
