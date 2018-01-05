package se.devscout.achievements.server.auth.openid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.TokenServiceException;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.resources.OpenIdCallbackStateTokenService;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class EmailIdentityProvider implements IdentityProvider {

    private final OpenIdCallbackStateTokenService jwtTokenService;
    private EmailSender emailSender;
    private CredentialsValidator tokenValidator;

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailIdentityProvider.class);

    public EmailIdentityProvider(JwtTokenService jwtTokenService, EmailSender emailSender) {
        this.jwtTokenService = new OpenIdCallbackStateTokenService(jwtTokenService);
        this.emailSender = emailSender;
        this.tokenValidator = new EmailTokenValidator(jwtTokenService);
    }

    @Override
    public URI getProviderAuthURL(String callbackState, URI callbackUri) throws IdentityProviderException {

        final URI confirmationUri = getSignInLink(callbackUri, callbackState);

        try {

            final String email = jwtTokenService.decode(callbackState).getEmail();

            sendEmail(email, confirmationUri);

            //TODO: Do not hard-code hostname here:
            return URI.create("http://localhost:63344/#signin/check-mail-box");
        } catch (TokenServiceException e) {
            //TODO: Unit test for when TokenServiceException happens
            LOGGER.warn("Could not read JWT token containing e-mail address", e);
            throw new IdentityProviderException("Could not read JWT token containing e-mail address", e);
        } catch (EmailSenderException e) {
            //TODO: Unit test for when EmailSenderException happens
            LOGGER.warn("Could not send link by mail", e);
            throw new IdentityProviderException("Could not send link by mail", e);
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

    private URI getSignInLink(URI path, String callbackState) {
        return UriBuilder.fromUri(path)
                // Yes, use callbackState as code.
                .queryParam("code", callbackState)
                .build();
    }

    @Override
    public ValidationResult handleCallback(String authCode, URI callbackUri) {
        return tokenValidator.validate(authCode.toCharArray());
    }

}
