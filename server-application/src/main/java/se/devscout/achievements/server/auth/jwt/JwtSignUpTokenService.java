package se.devscout.achievements.server.auth.jwt;

import se.devscout.achievements.server.resources.UuidString;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class JwtSignUpTokenService {

    private static final String ORGANIZATION_ID = "organizationId";
    private static final String ORGANIZATION_NAME = "organizationName";
    private static final Duration DURATION_15_MINS = Duration.ofMinutes(15);

    private final JwtTokenService jwtTokenService;

    public JwtSignUpTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public String encode(JwtSignUpToken state) {
        final Map<String, String> claims = new HashMap<>();
        if (state.getOrganizationId() != null) {
            claims.put(ORGANIZATION_ID, state.getOrganizationId().getValue());
        }
        if (state.getOrganizationName() != null) {
            claims.put(ORGANIZATION_NAME, state.getOrganizationName());
        }
        return jwtTokenService.encode(null, claims, DURATION_15_MINS);
    }

    public JwtSignUpToken decode(String token) throws JwtTokenServiceException {
        final var decodedJWT = jwtTokenService.decode(token);
        final var claims = decodedJWT.getClaims();
        return new JwtSignUpToken(
                claims.containsKey(ORGANIZATION_ID) ? new UuidString(claims.get(ORGANIZATION_ID).asString()) : null,
                claims.containsKey(ORGANIZATION_NAME) ? claims.get(ORGANIZATION_NAME).asString() : null
        );
    }
}
