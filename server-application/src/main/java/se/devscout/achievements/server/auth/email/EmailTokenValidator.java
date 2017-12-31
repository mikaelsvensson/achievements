package se.devscout.achievements.server.auth.email;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.model.CredentialsType;

public class EmailTokenValidator implements CredentialsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTokenValidator.class);

    @Override
    public ValidationResult validate(char[] data) {
        final String token = new String(data);
        //TODO: Verify JWT, don't just decode it.
        final DecodedJWT jwt = JWT.decode(token);
        final String email = jwt.getClaim("email").asString();
        return new ValidationResult(email, email, true);
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
