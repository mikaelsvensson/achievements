package se.devscout.achievements.server;

import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.CredentialsDaoImpl;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;
import se.devscout.achievements.server.auth.PasswordValidator;

import java.io.IOException;

public class App extends AchievementsApplication {

    @Override
    protected CredentialsDao getCredentialsDao(SessionFactory sessionFactory) {
        final CredentialsDao dao = new CredentialsDaoImpl(sessionFactory) {
            @Override
            public Credentials get(IdentityProvider provider, String username) {
                try {
                    return new Credentials(username, new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return dao;
    }
}
