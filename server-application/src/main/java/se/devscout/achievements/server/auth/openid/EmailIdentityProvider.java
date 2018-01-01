package se.devscout.achievements.server.auth.openid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.TokenServiceException;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.resources.OpenIdCallbackStateTokenService;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class EmailIdentityProvider extends AbstractIdentityProvider {

    private final OpenIdCallbackStateTokenService jwtTokenService;
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
        this.jwtTokenService = new OpenIdCallbackStateTokenService(jwtTokenService);
        this.emailSender = emailSender;
    }

    @Override
    public URI getCallbackURL(String path) {
        return URI.create("http://localhost:8080/api/openid/password/" + path);
    }

    @Override
    public URI getProviderAuthURL(String path, String callbackState) {

        final URI confirmationUri = getSignInLink(path, callbackState);

        try {

            final String email = jwtTokenService.decode(callbackState).getEmail();

            sendEmail(email, confirmationUri);

            return URI.create("http://localhost:63344/#signin/check-mail-box");
        } catch (TokenServiceException e) {
            //TODO: Unit test for when TokenServiceException happens
            LOGGER.warn("Could not read JWT token containing e-mail address", e);
            return URI.create("http://localhost:63344/#signin/failed");
        } catch (EmailSenderException e) {
            //TODO: Unit test for when EmailSenderException happens
            LOGGER.warn("Could not send link by mail", e);
            return URI.create("http://localhost:63344/#signin/failed");
        }
    }

    private void sendEmail(String to, URI confirmationUri) throws EmailSenderException {
        final String link = confirmationUri.toString();
        LOGGER.info("Confirmation link: " + link);

        //TODO: Localize e-mail
        emailSender.send(to, "Sign in to Achievements", getMessageBody(link));

        LOGGER.info("Sent this link to {}: {}", to, link);
    }

    private String getMessageBody(String link) {
        return "Use this link to sign in:\n" + link;
    }

    private URI getSignInLink(String path, String callbackState) {
        return UriBuilder.fromUri("http://localhost:8080/api/openid/password")
                .path(path)
                // Yes, use callbackState as code.
                .queryParam("code", callbackState)
                .build();
    }

    @Override
    public ValidationResult handleCallback(String authCode, String path) {
        return parseToken(authCode);
    }
}
