package se.devscout.achievements.server.auth.jwt;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import se.devscout.achievements.server.resources.UuidString;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public class JwtSignInTokenService {

    public static final String ORGANIZATION_ID = "organizationId";
    public static final String EMAIL = "email";
    public static final Duration DEFUALT_TOKEN_VALIDITY_DURATION = Duration.ofMinutes(15);

    private final JwtTokenService jwtTokenService;
    private final Duration tokenValidityDuration;

    public JwtSignInTokenService(JwtTokenService jwtTokenService) {
        this(jwtTokenService, DEFUALT_TOKEN_VALIDITY_DURATION);
    }

    public JwtSignInTokenService(JwtTokenService jwtTokenService, Duration tokenValidityDuration) {
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService);
        this.tokenValidityDuration = Objects.requireNonNullElse(tokenValidityDuration, DEFUALT_TOKEN_VALIDITY_DURATION);
    }

    public String encode(JwtSignInToken token) {
        final var claims = ImmutableMap.of(
                "credentials", token.getCredentialsId().toString(),
                "id", String.valueOf(token.getPersonId()),
                "organization", new UuidString(token.getOrganizationId()).getValue(),
                "roles", Joiner.on(' ').join(token.getRoles()));

        return jwtTokenService.encode(token.getPersonName(), claims, tokenValidityDuration);
    }

    public JwtSignInToken decode(String token) throws JwtTokenServiceException {

        final var jwt = jwtTokenService.decode(token);

        final var roles = Sets.newHashSet(Splitter.on(' ').split(jwt.getClaim("roles").asString()));
        return new JwtSignInToken(
                jwt.getSubject(),
                Integer.parseInt(jwt.getClaim("id").asString()),
                UUID.fromString(jwt.getClaim("credentials").asString()),
                roles,
                new UuidString(jwt.getClaim("organization").asString()).getUUID());

    }
}
