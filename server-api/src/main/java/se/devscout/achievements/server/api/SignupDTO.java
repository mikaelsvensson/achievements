package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.devscout.achievements.server.data.model.IdentityProvider;

public class SignupDTO extends SignupBaseDTO {
    public String new_organization_name;

    public SignupDTO() {
        super(null, null, null);
    }

    public SignupDTO(@JsonProperty("email") String email,
                     @JsonProperty("identity_provider_data") String identityProviderData,
                     @JsonProperty("new_organization_name") String newOrganizationName,
                     @JsonProperty("identity_provider") IdentityProvider identityProvider) {
        super(email, identityProviderData, identityProvider);
        this.new_organization_name = newOrganizationName;
    }
}
