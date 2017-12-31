package se.devscout.achievements.server.auth.jwt;

public class TokenServiceException extends Exception {
    public TokenServiceException(Exception cause) {
        super(cause);
    }
}
