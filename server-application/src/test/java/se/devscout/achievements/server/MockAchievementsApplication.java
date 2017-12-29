package se.devscout.achievements.server;

import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.CredentialsDaoImpl;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;

import java.io.IOException;

import static se.devscout.achievements.server.MockUtil.mockOrganization;
import static se.devscout.achievements.server.MockUtil.mockPerson;

public class MockAchievementsApplication extends AchievementsApplication {

    @Override
    protected CredentialsDao getCredentialsDao(SessionFactory sessionFactory) {
        final CredentialsDao dao = new CredentialsDaoImpl(sessionFactory) {
            @Override
            public Credentials get(IdentityProvider provider, String username) {
                try {
                    final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
                    final Organization organization = mockOrganization("Acme Inc.");
                    final Person person = mockPerson(organization, "Alice");
                    final Credentials credentials = new Credentials("username", passwordValidator.getIdentityProvider(), passwordValidator.getSecret(), person);
                    return credentials;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return dao;
    }
}
