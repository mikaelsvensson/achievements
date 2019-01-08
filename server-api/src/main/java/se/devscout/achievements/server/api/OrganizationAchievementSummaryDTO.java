package se.devscout.achievements.server.api;

import java.util.ArrayList;
import java.util.List;

public class OrganizationAchievementSummaryDTO {
    public List<AchievementSummaryDTO> achievements = new ArrayList<>();

    public static class AchievementSummaryDTO {
        public AchievementBaseDTO achievement;
        public ProgressSummaryDTO progress_summary;
        public List<PersonProgressDTO> progress_detailed;
    }

    public static class ProgressSummaryDTO {
        // TODO: How many of these properties are actually used by the GUI?
        public int people_completed;
        public int people_started;
        public int people_awarded;
    }

    public static class PersonProgressDTO {
        public PersonBaseDTO person;
        public int percent;
        public boolean awarded;
    }
}
