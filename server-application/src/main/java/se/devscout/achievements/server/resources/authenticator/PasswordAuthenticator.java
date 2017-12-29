package se.devscout.achievements.server.resources.authenticator;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.hibernate.UnitOfWork;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.SecretValidationResult;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;

import java.util.Optional;

public class PasswordAuthenticator implements Authenticator<BasicCredentials, User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordAuthenticator.class);
    private final CredentialsDao credentialsDao;

    public PasswordAuthenticator(CredentialsDao credentialsDao) {
        this.credentialsDao = credentialsDao;
    }

    @Override
    @UnitOfWork
    public Optional<User> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
        try {
            final Credentials credentials = credentialsDao.get(IdentityProvider.PASSWORD, basicCredentials.getUsername());
            final SecretValidationResult validationResult = new PasswordValidator(credentials.getSecret()).validate(basicCredentials.getPassword().toCharArray());
            if (validationResult.isValid()) {
                final User user = new User(
                        credentials.getPerson().getId(),
                        credentials.getId(),
                        basicCredentials.getUsername());
                return Optional.of(user);
            } else {
                return Optional.empty();
            }
        } catch (HibernateException | ObjectNotFoundException e) {
            LOGGER.error("Exception when trying to validate credentials", e);
            return Optional.empty();
        }
    }
}
