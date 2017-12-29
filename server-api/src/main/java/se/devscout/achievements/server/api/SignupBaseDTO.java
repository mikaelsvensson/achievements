package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.devscout.achievements.server.data.model.IdentityProvider;

public class SignupBaseDTO {
    public IdentityProvider identity_provider;
    public String identity_provider_data;
    public String email;

    public SignupBaseDTO(@JsonProperty("email") String email,
                         @JsonProperty("identity_provider_data") String identityProviderData,
                         @JsonProperty("identity_provider") IdentityProvider identityProvider) {
        this.identity_provider = identityProvider;
        this.identity_provider_data = identityProviderData;
        this.email = email;
    }
}
