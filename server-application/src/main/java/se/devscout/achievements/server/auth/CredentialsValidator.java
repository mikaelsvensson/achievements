package se.devscout.achievements.server.auth;

import se.devscout.achievements.server.data.model.CredentialsType;

public interface CredentialsValidator {
    ValidationResult validate(char[] data);

    byte[] getCredentialsData();

    CredentialsType getCredentialsType();
}
