package se.devscout.achievements.server.auth.openid;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.model.CredentialsType;

public class MicrosoftTokenValidator implements CredentialsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftTokenValidator.class);

    @Override
    public ValidationResult validate(char[] data) {
        final String token = new String(data);
        final DecodedJWT jwt = JWT.decode(token);
        //TODO: Look into how to guarantee that the e-mail claim is returned
        return new ValidationResult(jwt.getClaim("email").asString(), jwt.getSubject(), true, CredentialsType.MICROSOFT, new byte[0]);
    }

    @Override
    public byte[] getCredentialsData() {
        return new byte[0];
    }

    @Override
    public CredentialsType getCredentialsType() {
        return CredentialsType.MICROSOFT;
    }
}
