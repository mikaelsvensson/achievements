package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProgressDTO {

    public ProgressDTO() {
    }

    public ProgressDTO(@JsonProperty("completed") Boolean completed,
                       @JsonProperty("progress") Integer value,
                       @JsonProperty("note") String note) {
        this.completed = completed;
        this.value = value;
        this.note = note;
    }

    public Boolean completed;
    public Integer value;
    public String note;
}
