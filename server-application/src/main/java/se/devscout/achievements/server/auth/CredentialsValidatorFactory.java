package se.devscout.achievements.server.auth;

import se.devscout.achievements.server.auth.google.GoogleTokenValidator;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.model.CredentialsType;

public class CredentialsValidatorFactory {
    private String googleClientId;

    public CredentialsValidatorFactory(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public CredentialsValidator get(CredentialsType type, String credentialsData) {
        switch (type) {
            case PASSWORD:
                return new PasswordValidator(SecretGenerator.PDKDF2, credentialsData.toCharArray());
            case GOOGLE:
                return new GoogleTokenValidator(googleClientId);
            default:
                throw new IllegalArgumentException("Cannot handle credentials type " + type);
        }
    }
}
