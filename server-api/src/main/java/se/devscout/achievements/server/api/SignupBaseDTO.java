package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.devscout.achievements.server.data.model.CredentialsType;

public class SignupBaseDTO {
    public CredentialsType credentials_type;
    public String credentials_data;
    public String email;

    public SignupBaseDTO(@JsonProperty("email") String email,
                         @JsonProperty("credentials_data") String credentialsData,
                         @JsonProperty("credentials_type") CredentialsType credentialsType) {
        this.credentials_type = credentialsType;
        this.credentials_data = credentialsData;
        this.email = email;
    }
}
