package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ForgotPasswordDTO {
    public final String email;

    public ForgotPasswordDTO(@JsonProperty("email") String email) {
        this.email = email;
    }
}
