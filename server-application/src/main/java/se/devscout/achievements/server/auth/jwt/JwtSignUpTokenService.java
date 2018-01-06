package se.devscout.achievements.server.auth.jwt;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import se.devscout.achievements.server.resources.UuidString;

import java.util.HashMap;
import java.util.Map;

public class JwtSignUpTokenService {

    private static final String ORGANIZATION_ID = "organizationId";
    private static final String ORGANIZATION_NAME = "organizationName";
    private static final String EMAIL = "email";

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
        if (state.getEmail() != null) {
            claims.put(EMAIL, state.getEmail());
        }
        //TODO: Tokens should expire after, say, 15 minutes
        return jwtTokenService.encode(null, claims);
    }

    public JwtSignUpToken decode(String token) throws JwtTokenServiceException {
        final DecodedJWT decodedJWT = jwtTokenService.decode(token);
        final Map<String, Claim> claims = decodedJWT.getClaims();
        return new JwtSignUpToken(
                claims.containsKey(EMAIL) ? claims.get(EMAIL).asString() : null,
                claims.containsKey(ORGANIZATION_ID) ? new UuidString(claims.get(ORGANIZATION_ID).asString()) : null,
                claims.containsKey(ORGANIZATION_NAME) ? claims.get(ORGANIZATION_NAME).asString() : null
        );
    }
}
