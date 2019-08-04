package se.devscout.achievements.server.auth.email;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.I18n;
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
import se.devscout.achievements.server.mail.template.SigninTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

public class EmailIdentityProvider implements IdentityProvider {

    private final JwtEmailAddressTokenService jwtEmailAddressTokenService;
    private final SigninTemplate template = new SigninTemplate();
    private final I18n i18n;
    private EmailSender emailSender;

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailIdentityProvider.class);
    private URI guiApplicationHost;
    private final CredentialsDao credentialsDao;

    public EmailIdentityProvider(JwtTokenService jwtTokenService, EmailSender emailSender, URI guiApplicationHost, CredentialsDao credentialsDao) throws IOException {
        this.emailSender = emailSender;
        this.guiApplicationHost = guiApplicationHost;
        this.credentialsDao = credentialsDao;
        this.jwtEmailAddressTokenService = new JwtEmailAddressTokenService(jwtTokenService);

        this.i18n = new I18n("texts.sv.yaml");
    }

    @Override
    public URI getRedirectUri(HttpServletRequest req, HttpServletResponse resp, String callbackState, URI callbackUri) throws IdentityProviderException {
        try {
            final String email = req.getParameter("email");
            final String password = req.getParameter("password");
            final String clientId = req.getRemoteAddr() != null ? req.getRemoteAddr() : "ANONYMOUS";

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
            LOGGER.info("Could not perform authentication", e);
            throw new IdentityProviderException("Could not perform authentication", e);
        }
    }

    private void sendEmail(String clientId, String to, URI confirmationUri) throws EmailSenderException {
        LOGGER.info("Confirmation link: " + confirmationUri.toString());

        emailSender.send(
                clientId,
                to,
                i18n.get("emailIdentityProvider.email.subject"),
                //TODO: Localize e-mail
                template.render(confirmationUri, JwtEmailAddressTokenService.DURATION_15_MINS));

        LOGGER.info("Sent this link to {}: {}", to, confirmationUri.toString());
    }

    private URI getSignInLink(URI path, String callbackState, String email) {
        return UriBuilder.fromUri(path)
                .queryParam("code", jwtEmailAddressTokenService.encode(new JwtEmailAddressToken(email)))
                .queryParam("state", callbackState)
                .build();
    }

    @Override
    public ValidationResult handleCallback(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String authCode = req.getParameter("code");
            String callbackState = req.getParameter("state");
            final String email = jwtEmailAddressTokenService.decode(authCode).getEmail();
            return new ValidationResult(email, email, true, CredentialsType.PASSWORD, new byte[0], callbackState);
        } catch (JwtTokenServiceException e) {
            return ValidationResult.INVALID;
        }
    }

    @Override
    public Response getMetadataResponse() {
        return null;
    }

}
