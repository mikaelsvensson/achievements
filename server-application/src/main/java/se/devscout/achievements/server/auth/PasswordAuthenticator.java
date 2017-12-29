package se.devscout.achievements.server.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.hibernate.UnitOfWork;
import org.hibernate.HibernateException;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;

import java.util.Optional;

public class PasswordAuthenticator implements Authenticator<BasicCredentials, User> {

    private final CredentialsDao credentialsDao;

    public PasswordAuthenticator(CredentialsDao credentialsDao) {
        this.credentialsDao = credentialsDao;
    }

    @Override
    @UnitOfWork
    public Optional<User> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
        try {
            final Credentials credentials = credentialsDao.get(IdentityProvider.PASSWORD, basicCredentials.getUsername());
            final SecretValidator validator = new PasswordValidator(credentials.getSecret());
            final SecretValidationResult validationResult = validator.validate(basicCredentials.getPassword().toCharArray());
            if (validationResult.isValid()) {
                final User user = new User(
                        credentials.getPerson().getId(),
                        credentials.getId(),
                        basicCredentials.getUsername());
                return Optional.of(user);
            } else {
                return Optional.empty();
            }
        } catch (HibernateException e) {
            //TODO: Log exception
            return Optional.empty();
        } catch (ObjectNotFoundException e) {
            //TODO: Log exception
            return Optional.empty();
        }
    }
}
