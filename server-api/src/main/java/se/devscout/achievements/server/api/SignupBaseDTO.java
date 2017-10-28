package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignupBaseDTO {
    public String name;
    public String email;
    public String password;

    public SignupBaseDTO(@JsonProperty("name") String name,
                         @JsonProperty("email") String email,
                         @JsonProperty("password") String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
