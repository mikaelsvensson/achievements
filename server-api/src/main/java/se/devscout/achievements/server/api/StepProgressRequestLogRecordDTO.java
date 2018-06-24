package se.devscout.achievements.server.api;

import java.time.OffsetDateTime;

public class StepProgressRequestLogRecordDTO {
    public PersonBaseDTO user;
    public AchievementStepDTO step;
    public OffsetDateTime date_time;
    public ProgressDTO data;
    public String http_method;
    public int response_code;
}
