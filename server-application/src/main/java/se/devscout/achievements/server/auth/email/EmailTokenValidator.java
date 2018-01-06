package se.devscout.achievements.server.auth.email;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceException;
import se.devscout.achievements.server.data.model.CredentialsType;

public class EmailTokenValidator implements CredentialsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTokenValidator.class);
    private final JwtTokenService jwtTokenService;

    EmailTokenValidator(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public ValidationResult validate(char[] data) {
        try {
            final String token = new String(data);
            final DecodedJWT jwt = jwtTokenService.decode(token);
            final String email = jwt.getClaim("email").asString();
            return new ValidationResult(email, email, true, CredentialsType.PASSWORD, new byte[0]);
        } catch (JwtTokenServiceException e) {
            LOGGER.info("Could not decode JWT", e);
            return ValidationResult.INVALID;
        }
    }

    @Override
    public byte[] getCredentialsData() {
        return new byte[0];
    }

    @Override
    public CredentialsType getCredentialsType() {
        return CredentialsType.PASSWORD;
    }
}
