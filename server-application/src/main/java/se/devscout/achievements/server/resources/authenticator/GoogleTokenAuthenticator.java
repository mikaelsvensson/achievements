package se.devscout.achievements.server.resources.authenticator;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.SecretValidationResult;
import se.devscout.achievements.server.auth.google.GoogleTokenValidator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Reference: https://developers.google.com/identity/sign-in/web/backend-auth
 */
public class GoogleTokenAuthenticator implements Authenticator<String, User> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTokenAuthenticator.class);

    private final CredentialsDao credentialsDao;
    private final String googleClientId;

    public GoogleTokenAuthenticator(CredentialsDao credentialsDao, String googleClientId) {
        requireNonNull(credentialsDao);
        requireNonNull(googleClientId);

        this.credentialsDao = credentialsDao;
        this.googleClientId = googleClientId;
    }

    @Override
    @UnitOfWork
    public Optional<User> authenticate(String token) throws AuthenticationException {
        final SecretValidationResult validationResult = new GoogleTokenValidator(googleClientId).validate(token.toCharArray());
        if (validationResult.isValid()) {
            try {
                Credentials credentials = credentialsDao.get(IdentityProvider.GOOGLE, validationResult.getUserName());
                return Optional.of(new User(credentials.getPerson().getId(), credentials.getId(), validationResult.getUserEmail()));
            } catch (ObjectNotFoundException e) {
                LOGGER.error("Exception when trying to validate credentials", e);
                return Optional.empty();
            }
        } else {
            LOGGER.error("Could not validate token");
            return Optional.empty();
        }
    }
}
