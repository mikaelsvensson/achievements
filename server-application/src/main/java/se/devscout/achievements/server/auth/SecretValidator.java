package se.devscout.achievements.server.auth;

public interface SecretValidator {
    boolean validate(char[] secret);

    byte[] getSecret();
}
