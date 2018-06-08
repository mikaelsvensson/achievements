package se.devscout.achievements.server.auth.email;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtEmailAddressToken;
import se.devscout.achievements.server.auth.jwt.JwtEmailAddressTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceException;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;

public class EmailIdentityProvider implements IdentityProvider {

    private final JwtEmailAddressTokenService jwtEmailAddressTokenService;
    private EmailSender emailSender;

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailIdentityProvider.class);
    private URI guiApplicationHost;
    private final CredentialsDao credentialsDao;

    public EmailIdentityProvider(JwtTokenService jwtTokenService, EmailSender emailSender, URI guiApplicationHost, CredentialsDao credentialsDao) {
        this.emailSender = emailSender;
        this.guiApplicationHost = guiApplicationHost;
        this.credentialsDao = credentialsDao;
        this.jwtEmailAddressTokenService = new JwtEmailAddressTokenService(jwtTokenService);
    }

    @Override
    public URI getRedirectUri(String callbackState, URI callbackUri, Map<String, String> providerData) throws IdentityProviderException {
        try {
            if (providerData == null) {
                throw new IdentityProviderException("Email or password not specified.");
            }

            final String email = providerData.get("email");
            final String password = providerData.get("password");
            final String clientId = providerData.getOrDefault("ip", "ANYNOMOUS");

            if (Strings.isNullOrEmpty(email)) {
                throw new IdentityProviderException("Email not specified.");
            }

            final URI confirmationUri = getSignInLink(callbackUri, callbackState, email);

            if (!Strings.isNullOrEmpty(password)) {
                final Credentials credentials = credentialsDao.get(CredentialsType.PASSWORD, email);
                final PasswordValidator validator = new PasswordValidator(credentials.getData());
                final ValidationResult validationResult = validator.validate(password.toCharArray());

                if (validationResult.isValid()) {
                    return confirmationUri;
                } else {
                    return URI.create(StringUtils.appendIfMissing(guiApplicationHost.toString(), "/") + "#signin-failed/invalid-credentials");
                }
            } else {
                sendEmail(clientId, email, confirmationUri);

                return URI.create(StringUtils.appendIfMissing(guiApplicationHost.toString(), "/") + "#signin/check-mail-box");
            }
        } catch (EmailSenderException e) {
            //TODO: Unit test for when EmailSenderException happens
            LOGGER.warn("Could not send link by mail", e);
            throw new IdentityProviderException("Could not send link by mail", e);
        } catch (ObjectNotFoundException e) {
            //TODO: Unit test for when ObjectNotFoundException happens
            LOGGER.warn("Could not perform authentication", e);
            throw new IdentityProviderException("Could not perform authentication", e);
        }
    }

    private void sendEmail(String clientId, String to, URI confirmationUri) throws EmailSenderException {
        final String link = confirmationUri.toString();
        LOGGER.info("Confirmation link: " + link);

        //TODO: Localize e-mail
        emailSender.send(clientId, to, "Sign in to Achievements", getMessageBody(link));

        LOGGER.info("Sent this link to {}: {}", to, link);
    }

    private String getMessageBody(String link) {
        return "Use this link to sign in:\n" + link;
    }

    private URI getSignInLink(URI path, String callbackState, String email) {
        return UriBuilder.fromUri(path)
                .queryParam("code", jwtEmailAddressTokenService.encode(new JwtEmailAddressToken(email)))
                .queryParam("state", callbackState)
                .build();
    }

    @Override
    public ValidationResult handleCallback(String authCode, URI callbackUri) {
        try {
            final String email = jwtEmailAddressTokenService.decode(authCode).getEmail();
            return new ValidationResult(email, email, true, CredentialsType.PASSWORD, new byte[0]);
        } catch (JwtTokenServiceException e) {
            return ValidationResult.INVALID;
        }
    }

}
