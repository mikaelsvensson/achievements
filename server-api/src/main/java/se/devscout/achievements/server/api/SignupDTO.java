package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.devscout.achievements.server.data.model.CredentialsType;

public class SignupDTO extends SignupBaseDTO {
    public String new_organization_name;

    public SignupDTO() {
        super(null, null, null);
    }

    public SignupDTO(@JsonProperty("email") String email,
                     @JsonProperty("credentials_data") String credentialsData,
                     @JsonProperty("new_organization_name") String newOrganizationName,
                     @JsonProperty("credentials_type") CredentialsType credentialsType) {
        super(email, credentialsData, credentialsType);
        this.new_organization_name = newOrganizationName;
    }
}
