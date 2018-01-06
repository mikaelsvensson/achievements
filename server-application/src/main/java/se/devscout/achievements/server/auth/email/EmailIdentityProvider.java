package se.devscout.achievements.server.auth.email;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtSignUpTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceException;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class EmailIdentityProvider implements IdentityProvider {

    private final JwtSignUpTokenService jwtTokenService;
    private EmailSender emailSender;
    private CredentialsValidator tokenValidator;

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailIdentityProvider.class);
    private URI guiApplicationHost;

    public EmailIdentityProvider(JwtTokenService jwtTokenService, EmailSender emailSender, URI guiApplicationHost) {
        this.jwtTokenService = new JwtSignUpTokenService(jwtTokenService);
        this.emailSender = emailSender;
        this.tokenValidator = new EmailTokenValidator(jwtTokenService);
        this.guiApplicationHost = guiApplicationHost;
    }

    @Override
    public URI getRedirectUri(String callbackState, URI callbackUri) throws IdentityProviderException {

        final URI confirmationUri = getSignInLink(callbackUri, callbackState);

        try {

            final String email = jwtTokenService.decode(callbackState).getEmail();

            sendEmail(email, confirmationUri);

            return URI.create(StringUtils.appendIfMissing(guiApplicationHost.toString(), "/") + "#signin/check-mail-box");
        } catch (JwtTokenServiceException e) {
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
