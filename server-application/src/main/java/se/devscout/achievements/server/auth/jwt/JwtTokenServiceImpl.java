package se.devscout.achievements.server.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class JwtTokenServiceImpl implements JwtTokenService {

    private static final String ISSUER = "achievements";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenServiceImpl(String secret) {
        try {
            algorithm = Algorithm.HMAC512(secret);
            verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String encode(String subject, Map<String, String> claims) {
        JWTCreator.Builder builder = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(subject);
        if (claims != null) {
            for (Map.Entry<String, String> entry : claims.entrySet()) {
                if (entry.getValue() != null) {
                    builder = builder.withClaim(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder.sign(algorithm);
    }

    @Override
    public DecodedJWT decode(String token) throws TokenServiceException {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new TokenServiceException(e);
        }
    }
}