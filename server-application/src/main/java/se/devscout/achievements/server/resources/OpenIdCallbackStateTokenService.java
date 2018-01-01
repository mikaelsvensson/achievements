package se.devscout.achievements.server.resources;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.TokenServiceException;

import java.util.HashMap;
import java.util.Map;

public class OpenIdCallbackStateTokenService {

    private static final String ORGANIZATION_ID = "organizationId";
    private static final String ORGANIZATION_NAME = "organizationName";
    private static final String EMAIL = "email";

    private final JwtTokenService jwtTokenService;

    public OpenIdCallbackStateTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    String encode(OpenIdCallbackStateToken state) {
        final Map<String, String> claims = new HashMap<>();
        if (state.getOrganizationId() != null) {
            claims.put(ORGANIZATION_ID, state.getOrganizationId().getValue());
        }
        if (state.getOrganizationName() != null) {
            claims.put(ORGANIZATION_NAME, state.getOrganizationName());
        }
        if (state.getEmail() != null) {
            claims.put(EMAIL, state.getEmail());
        }
        //TODO: Tokens should expire after, say, 15 minutes
        return jwtTokenService.encode(null, claims);
    }

    public OpenIdCallbackStateToken decode(String token) throws TokenServiceException {
        final DecodedJWT decodedJWT = jwtTokenService.decode(token);
        final Map<String, Claim> claims = decodedJWT.getClaims();
        return new OpenIdCallbackStateToken(
                claims.containsKey(EMAIL) ? claims.get(EMAIL).asString() : null,
                claims.containsKey(ORGANIZATION_ID) ? new UuidString(claims.get(ORGANIZATION_ID).asString()) : null,
                claims.containsKey(ORGANIZATION_NAME) ? claims.get(ORGANIZATION_NAME).asString() : null
        );
    }
}
