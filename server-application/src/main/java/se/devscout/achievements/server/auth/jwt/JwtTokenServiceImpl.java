package se.devscout.achievements.server.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtTokenServiceImpl implements JwtTokenService {

    private static final String ISSUER = "achievements";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenServiceImpl(String secret) {
        algorithm = Algorithm.HMAC512(secret);
        verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
    }

    @Override
    public String encode(String subject, Map<String, String> claims, Duration validFor) {
        var builder = JWT.create()
                .withExpiresAt(new Date(Instant.now().plus(validFor).getEpochSecond() * 1000))
                .withIssuer(ISSUER)
                .withSubject(subject);
        if (claims != null) {
            for (var entry : claims.entrySet()) {
                if (entry.getValue() != null) {
                    builder = builder.withClaim(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder.sign(algorithm);
    }

    @Override
    public DecodedJWT decode(String token) throws JwtTokenServiceException {
        try {
            return verifier.verify(token);
        } catch (TokenExpiredException e) {
            throw new JwtTokenExpiredException(e);
        } catch (JWTVerificationException e) {
            throw new JwtTokenServiceException(e);
        }
    }
}
