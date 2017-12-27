package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthTokenDTO {
    public String token;

    public AuthTokenDTO(@JsonProperty("token") String token) {
        this.token = token;
    }
}
