package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnsuccessfulDTO {
    public final String message;

    public final int status;

    public UnsuccessfulDTO(@JsonProperty("message") String message,
                           @JsonProperty("status") int status) {
        this.message = message;
        this.status = status;
    }
}
