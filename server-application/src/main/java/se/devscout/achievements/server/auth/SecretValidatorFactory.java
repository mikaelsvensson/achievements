package se.devscout.achievements.server.auth;

import se.devscout.achievements.server.data.model.GoogleTokenValidator;
import se.devscout.achievements.server.data.model.IdentityProvider;

import java.io.IOException;

public class SecretValidatorFactory {
    private String googleClientId;

    public SecretValidatorFactory(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public SecretValidator get(IdentityProvider idp, String generatorConfiguration) {
        switch (idp) {
            case PASSWORD:
                try {
                    return new PasswordValidator(SecretGenerator.PDKDF2, generatorConfiguration.toCharArray());
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            case GOOGLE:
                return new GoogleTokenValidator(googleClientId);
            default:
                throw new IllegalArgumentException("Cannot handle identity provider " + idp);
        }
    }
}
