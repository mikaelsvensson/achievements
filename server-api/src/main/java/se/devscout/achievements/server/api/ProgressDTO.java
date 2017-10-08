package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProgressDTO {

    public ProgressDTO(@JsonProperty("completed") boolean completed,
                       @JsonProperty("note") String note) {
        this.completed = completed;
        this.note = note;
    }

    public boolean completed;
    public String note;
}
