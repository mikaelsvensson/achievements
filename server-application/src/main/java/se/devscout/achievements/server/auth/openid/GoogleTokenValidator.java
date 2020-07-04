package se.devscout.achievements.server.auth.openid;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.model.CredentialsType;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GoogleTokenValidator implements CredentialsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTokenValidator.class);
    private final String googleClientId;

    public GoogleTokenValidator(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    @Override
    public ValidationResult validate(char[] data) {
        var verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            var token = new String(data);
            var idToken = verifier.verify(token);
            if (idToken != null) {
                var payload = idToken.getPayload();

                var userId = payload.getSubject();

                var email = payload.getEmail();
                boolean emailVerified = payload.getEmailVerified();
                var name = (String) payload.get("name");
                var pictureUrl = (String) payload.get("picture");
                var locale = (String) payload.get("locale");
                LOGGER.info("User authenticated using Google, email={0}, emailVerified={1}, name={2}, locale={3}, picture={4}",
                        email,
                        emailVerified,
                        name,
                        locale,
                        pictureUrl);

                return new ValidationResult(email, userId, true, CredentialsType.GOOGLE, new byte[0]);
            } else {
                LOGGER.info("Invalid Google token");
                return ValidationResult.INVALID;
            }
        } catch (GeneralSecurityException | IOException e) {
            return ValidationResult.INVALID;
        }
    }

    @Override
    public byte[] getCredentialsData() {
        return new byte[0];
    }

    @Override
    public CredentialsType getCredentialsType() {
        return CredentialsType.GOOGLE;
    }
}
