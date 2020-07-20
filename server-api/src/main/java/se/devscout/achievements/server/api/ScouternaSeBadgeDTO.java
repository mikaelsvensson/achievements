package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScouternaSeBadgeDTO {
    public AchievementDTO fromScouternaSe;
    public AchievementDTO fromDatabase;

    public ScouternaSeBadgeDTO() {
    }

    public ScouternaSeBadgeDTO(
            @JsonProperty("from_scouterna_se") AchievementDTO fromScouternaSe,
            @JsonProperty("from_database") AchievementDTO fromDatabase) {
        this.fromScouternaSe = fromScouternaSe;
        this.fromDatabase = fromDatabase;
    }
}
