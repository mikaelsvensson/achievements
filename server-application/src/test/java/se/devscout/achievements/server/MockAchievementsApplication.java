package se.devscout.achievements.server;

import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.CredentialsDaoImpl;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;

import static se.devscout.achievements.server.MockUtil.*;

public class MockAchievementsApplication extends AchievementsApplication {

    @Override
    protected CredentialsDao getCredentialsDao(SessionFactory sessionFactory) {
        final CredentialsDao dao = new CredentialsDaoImpl(sessionFactory) {

            final Organization organization = mockOrganization("Acme Inc.");
            final Person personReader = mockPerson(organization, "Alice Reader", Roles.READER);
            final Person personEditor = mockPerson(organization, "Alice Editor", Roles.EDITOR);

            @Override
            public Credentials get(CredentialsType type, String userId) throws ObjectNotFoundException {
                if (USERNAME_EDITOR.equals(userId)) {
                    final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
                    final Credentials credentials = new Credentials(USERNAME_EDITOR, passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData(), personEditor);
                    return credentials;
                } else if (USERNAME_READER.equals(userId)) {
                    final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
                    final Credentials credentials = new Credentials(USERNAME_READER, passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData(), personReader);
                    return credentials;
                } else {
                    throw new ObjectNotFoundException();
                }

            }
        };
        return dao;
    }
}
