package se.devscout.achievements.server.auth.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Map;

public interface JwtTokenService {
    String encode(String subject, Map<String, String> claims);

    DecodedJWT decode(String token) throws TokenServiceException;
}
