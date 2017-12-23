package se.devscout.achievements.server.api;

import java.util.ArrayList;
import java.util.List;

public class OrganizationAchievementSummaryDTO {
    public List<AchievementSummaryDTO> achievements = new ArrayList<>();

    public static class AchievementSummaryDTO {
        public AchievementBaseDTO achievement;
        public ProgressSummaryDTO progress_summary;
    }

    public static class ProgressSummaryDTO {
        public int people_completed;
        public int people_started;
    }
}
