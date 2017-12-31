package se.devscout.achievements.server.auth.openid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.email.EmailTokenValidator;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class EmailIdentityProvider extends AbstractIdentityProvider {


    private static final Logger LOGGER = LoggerFactory.getLogger(EmailIdentityProvider.class);

    public EmailIdentityProvider() {
        super(
                null,
                null,
                null,
                null,
                null,
                new EmailTokenValidator());
    }

    @Override
    public URI getCallbackURL(String path) {
        return URI.create("http://localhost:8080/api/openid/password/" + path);
    }

    @Override
    public URI getProviderAuthURL(String path, String appState) {
        //TODO: Send e-mail with authentication link
        final URI confirmationUri = UriBuilder.fromUri("http://localhost:8080/api/openid/password")
                .path(path)
                .queryParam("code", appState)
                .queryParam("state", appState)
                .build();

        LOGGER.info(confirmationUri.toString());

        return URI.create("http://localhost:63344/#signin/check-mail-box");
    }

    @Override
    public ValidationResult handleCallback(String authCode, String path) {
        return parseToken(authCode);
    }
}
