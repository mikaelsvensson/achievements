package se.devscout.achievements.server.auth.openid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.email.EmailTokenValidator;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.TokenServiceException;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class EmailIdentityProvider extends AbstractIdentityProvider {

    private final JwtTokenService jwtTokenService;
    private EmailSender emailSender;

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailIdentityProvider.class);

    public EmailIdentityProvider(JwtTokenService jwtTokenService, EmailSender emailSender) {
        super(
                null,
                null,
                null,
                null,
                null,
                new EmailTokenValidator(jwtTokenService));
        this.jwtTokenService = jwtTokenService;
        this.emailSender = emailSender;
    }

    @Override
    public URI getCallbackURL(String path) {
        return URI.create("http://localhost:8080/api/openid/password/" + path);
    }

    @Override
    public URI getProviderAuthURL(String path, String appState) {
        final URI confirmationUri = UriBuilder.fromUri("http://localhost:8080/api/openid/password")
                .path(path)
                // Yes, use appState as code.
                .queryParam("code", appState)
                .build();

        final String link = confirmationUri.toString();

        LOGGER.info("Confirmation link: " + link);

        try {
            final String email = jwtTokenService.decode(appState).getClaim("email").asString();

            //TODO: Localize e-mail
            emailSender.send(email, "Sign in to Achievements", "Use this link to sign in:\n" + link);

            LOGGER.info("Sent this link to {}: {}", email, link);
        } catch (TokenServiceException e) {
            //TODO: Unit test for when TokenServiceException happens
            LOGGER.warn("Could not read JWT token containing e-mail address", e);
        } catch (EmailSenderException e) {
            //TODO: Unit test for when EmailSenderException happens
            LOGGER.warn("Could not send link by mail", e);
        }

        //TODO: Redirect to error page if exception is thrown
        return URI.create("http://localhost:63344/#signin/check-mail-box");
    }

    @Override
    public ValidationResult handleCallback(String authCode, String path) {
        return parseToken(authCode);
    }
}
