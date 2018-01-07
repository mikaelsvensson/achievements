package se.devscout.achievements.server.auth.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Duration;
import java.util.Map;

public interface JwtTokenService {
    String encode(String subject, Map<String, String> claims, Duration validFor);

    DecodedJWT decode(String token) throws JwtTokenServiceException;
}
