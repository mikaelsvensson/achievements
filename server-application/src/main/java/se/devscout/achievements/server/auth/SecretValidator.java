package se.devscout.achievements.server.auth;

import se.devscout.achievements.server.data.model.IdentityProvider;

public interface SecretValidator {
    SecretValidationResult validate(char[] secret);

    byte[] getSecret();

    IdentityProvider getIdentityProvider();
}
