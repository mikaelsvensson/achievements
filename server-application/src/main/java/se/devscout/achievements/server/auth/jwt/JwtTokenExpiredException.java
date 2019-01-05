package se.devscout.achievements.server.auth.jwt;

public class JwtTokenExpiredException extends JwtTokenServiceException {
    public JwtTokenExpiredException(Exception cause) {
        super(cause);
    }
}
