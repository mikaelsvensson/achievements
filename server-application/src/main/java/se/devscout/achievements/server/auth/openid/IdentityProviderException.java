package se.devscout.achievements.server.auth.openid;

public class IdentityProviderException extends Exception {
    public IdentityProviderException(String message, Exception cause) {
        super(message, cause);
    }
}
