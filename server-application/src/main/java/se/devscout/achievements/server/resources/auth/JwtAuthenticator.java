package se.devscout.achievements.server.resources.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.jwt.JwtSignInToken;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenExpiredException;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceException;

import java.util.Optional;

public class JwtAuthenticator implements Authenticator<String, User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticator.class);
    private final JwtSignInTokenService jwtTokenService;

    public JwtAuthenticator(JwtSignInTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        try {
            final JwtSignInToken jwt = jwtTokenService.decode(token);
            final User user = new User(
                    jwt.getPersonId(),
                    jwt.getCredentialsId(),
                    jwt.getPersonName(),
                    jwt.getRoles(),
                    null);
            return Optional.of(user);
        } catch (JwtTokenExpiredException e) {
            LOGGER.info("Authentication token has expired", e);
            return Optional.empty();
        } catch (JwtTokenServiceException e) {
            LOGGER.warn("Exception when trying to validate credentials", e);
            return Optional.empty();
        }
    }
}
