package se.devscout.achievements.server.resources.authenticator;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.TokenServiceException;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class JwtAuthenticator implements Authenticator<String, User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticator.class);
    private final JwtTokenService tokenService;

    public JwtAuthenticator(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        try {
            final DecodedJWT jwt = tokenService.decode(token);
            final User user = new User(
                    Integer.parseInt(jwt.getClaim("id").asString()),
                    UUID.fromString(jwt.getClaim("credentials").asString()),
                    jwt.getSubject(),
                    Sets.newHashSet(Splitter.on(' ').split(jwt.getClaim("roles").asString())));
            return Optional.of(user);
        } catch (TokenServiceException e) {
            LOGGER.error("Exception when trying to validate credentials", e);
            return Optional.empty();
        }
    }

    public String generateToken(String user, UUID credentialsId, Integer personId, String role) {
        return tokenService.encode(user, ImmutableMap.of(
                "credentials", credentialsId.toString(),
                "id", String.valueOf(personId),
                "roles", Joiner.on(' ').join(Sets.union(Collections.singleton(role), Roles.IMPLICIT_ROLES.getOrDefault(role, Collections.emptySet())))));
    }

}
