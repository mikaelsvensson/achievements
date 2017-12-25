package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignupBaseDTO {
    public String email;
    public String password;

    public SignupBaseDTO(@JsonProperty("email") String email,
                         @JsonProperty("password") String password) {
        this.email = email;
        this.password = password;
    }
}
