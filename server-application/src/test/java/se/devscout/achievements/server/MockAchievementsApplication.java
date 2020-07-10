package se.devscout.achievements.server;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;

import java.util.stream.IntStream;

import static se.devscout.achievements.server.MockUtil.*;

public class MockAchievementsApplication extends AchievementsApplication {

    @Override
    protected CredentialsDao getCredentialsDao(SessionFactory sessionFactory) {

        final var dao = super.getCredentialsDao(sessionFactory);

        try (var session = sessionFactory.openSession()) {
            ManagedSessionContext.bind(session);
            var transaction = session.beginTransaction();
            try {

                final var org = new Organization("Acme Inc.");
                session.save(org);

                addUser("Alice Reader", USERNAME_READER, "password", "one-time-password-1", org, session, Roles.READER);
                addUser("Alice Editor", USERNAME_EDITOR, "password", "one-time-password-2", org, session, Roles.EDITOR);
                addUser("Alice Admin", USERNAME_ADMIN, "password", "one-time-password-3", org, session, Roles.ADMIN);

                // We create a bunch of one-time passwords here so that each test for the one-time password feature
                // can use its own password. Tests cannot share credentials since one-time credentials are
                // removed after use.
                IntStream.rangeClosed(1, 5).forEach(i -> addUser(
                        "Bob Builder" + i,
                        "onetimer" + i,
                        "password",
                        "onetimepassword" + i,
                        org,
                        session, Roles.READER));

                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
        }
        return dao;
    }

    private void addUser(String name, String username, String regularPassword, String onetimePassword, Organization organization, Session session, String role) {
        final var personReader = new Person(name, role);
        personReader.setOrganization(organization);
        session.save(personReader);

        final var passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, regularPassword.toCharArray());
        final var credentialsReader1 = new Credentials(username, passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData(), personReader);
        credentialsReader1.setPerson(personReader);
        session.save(credentialsReader1);

        final var credentialsReader2 = new Credentials(onetimePassword, CredentialsType.ONETIME_PASSWORD, null, personReader);
        credentialsReader2.setPerson(personReader);
        session.save(credentialsReader2);
    }
}
