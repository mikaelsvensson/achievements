package se.devscout.achievements.server.auth.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import se.devscout.achievements.server.resources.UuidString;

import java.time.Duration;
import java.util.HashSet;
import java.util.UUID;

public class JwtSignInTokenService {

    public static final String ORGANIZATION_ID = "organizationId";
    public static final String EMAIL = "email";
    private final static Duration TOKEN_VALIDITY_DURATION = Duration.ofMinutes(15);

    private final JwtTokenService jwtTokenService;

    public JwtSignInTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public String encode(JwtSignInToken token) {
        final ImmutableMap<String, String> claims = ImmutableMap.of(
                "credentials", token.getCredentialsId().toString(),
                "id", String.valueOf(token.getPersonId()),
                "organization", new UuidString(token.getOrganizationId()).getValue(),
                "roles", Joiner.on(' ').join(token.getRoles()));

        return jwtTokenService.encode(token.getPersonName(), claims, TOKEN_VALIDITY_DURATION);
    }

    public JwtSignInToken decode(String token) throws JwtTokenServiceException {

        final DecodedJWT jwt = jwtTokenService.decode(token);

        final HashSet<String> roles = Sets.newHashSet(Splitter.on(' ').split(jwt.getClaim("roles").asString()));
        return new JwtSignInToken(
                jwt.getSubject(),
                Integer.parseInt(jwt.getClaim("id").asString()),
                UUID.fromString(jwt.getClaim("credentials").asString()),
                roles,
                new UuidString(jwt.getClaim("organization").asString()).getUUID());

    }
}
