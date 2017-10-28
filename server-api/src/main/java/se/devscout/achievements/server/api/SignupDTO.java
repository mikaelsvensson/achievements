package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignupDTO extends SignupBaseDTO {
    public String new_organization_name;

    public SignupDTO() {
        super(null, null, null);
    }

    public SignupDTO(@JsonProperty("name") String name,
                     @JsonProperty("email") String email,
                     @JsonProperty("password") String password,
                     @JsonProperty("new_organization_name") String newOrganizationName) {
        super(name, email, password);
        this.new_organization_name = newOrganizationName;
    }
}
