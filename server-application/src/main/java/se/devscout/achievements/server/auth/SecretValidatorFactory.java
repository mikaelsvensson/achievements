package se.devscout.achievements.server.auth;

import se.devscout.achievements.server.auth.google.GoogleTokenValidator;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.model.IdentityProvider;

public class SecretValidatorFactory {
    private String googleClientId;

    public SecretValidatorFactory(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public SecretValidator get(IdentityProvider idp, String generatorConfiguration) {
        switch (idp) {
            case PASSWORD:
                return new PasswordValidator(SecretGenerator.PDKDF2, generatorConfiguration.toCharArray());
            case GOOGLE:
                return new GoogleTokenValidator(googleClientId);
            default:
                throw new IllegalArgumentException("Cannot handle identity provider " + idp);
        }
    }
}
