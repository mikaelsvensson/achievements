package se.devscout.achievements.server.auth.jwt;

public class JwtTokenServiceException extends Exception {
    public JwtTokenServiceException(Exception cause) {
        super(cause);
    }
}
