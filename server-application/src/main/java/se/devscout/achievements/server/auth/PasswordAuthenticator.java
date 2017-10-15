package se.devscout.achievements.server.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;

import java.util.Optional;

public class PasswordAuthenticator implements Authenticator<BasicCredentials, User> {

    private final SessionFactory sessionFactory;
    private final CredentialsDao credentialsDao;

    public PasswordAuthenticator(SessionFactory sessionFactory, CredentialsDao credentialsDao) {
        this.sessionFactory = sessionFactory;
        this.credentialsDao = credentialsDao;
    }

    @Override
    public Optional<User> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
        Optional<User> result = Optional.empty();
        Session session = null;
        try {
            session = sessionFactory.openSession();
            final Credentials credentials = credentialsDao.get(IdentityProvider.PASSWORD, basicCredentials.getUsername());
            if (credentials.getSecretValidator().validate(basicCredentials.getPassword().toCharArray())) {
                return Optional.of(new User(basicCredentials.getUsername()));
            }
            return Optional.empty();
        } catch (HibernateException e) {
            //TODO: Log exception
            return Optional.empty();
        } catch (ObjectNotFoundException e) {
            //TODO: Log exception
            return Optional.empty();
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
