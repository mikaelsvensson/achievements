package se.devscout.achievements.server.api;

public class AchievementStepDTO {
    public Integer id;

    public String prerequisite_achievement;

    public String description;

    public AchievementStepDTO() {
    }

    public AchievementStepDTO(String description) {
        this.description = description;
    }

    public static AchievementStepDTO withPrerequisite(String id) {
        final AchievementStepDTO dto = new AchievementStepDTO();
        dto.prerequisite_achievement = id;
        return dto;
    }

    public static AchievementStepDTO withDescription(String description) {
        final AchievementStepDTO dto = new AchievementStepDTO();
        dto.description = description;
        return dto;
    }
}
