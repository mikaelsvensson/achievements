package se.devscout.achievements.server.auth;

public class IdentityProviderException extends Exception {
    public IdentityProviderException(String message, Exception cause) {
        super(message, cause);
    }
}
