package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.devscout.achievements.server.data.model.StringSegment;

import java.util.List;

public class ScouternaSeBadgeDTO {
    public AchievementDTO fromScouternaSe;
    public AchievementDTO fromDatabase;
    public List<StringSegment> diffs;
    public long affected_people_count;

    public ScouternaSeBadgeDTO() {
    }

    public ScouternaSeBadgeDTO(
            @JsonProperty("from_scouterna_se") AchievementDTO fromScouternaSe,
            @JsonProperty("from_database") AchievementDTO fromDatabase,
            @JsonProperty("diffs") List<StringSegment> diffs,
            @JsonProperty("affected_people_count") long affectedPeopleCount) {
        this.fromScouternaSe = fromScouternaSe;
        this.fromDatabase = fromDatabase;
        this.diffs = diffs;
        this.affected_people_count = affectedPeopleCount;
    }
}
