package se.devscout.achievements.server.data.dao;

import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.auth.SecretValidator;
import se.devscout.achievements.server.data.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CredentialsDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Credentials.class)
            .addEntityClass(PasswordValidator.class)
            .addEntityClass(Organization.class)
            .addEntityClass(Person.class)
            .addEntityClass(PersonAttribute.class)
            .build();

    private CredentialsDaoImpl dao;
    private PeopleDao peopleDao;
    private OrganizationsDao organizationsDao;

    private Organization organization;
    private Person alice;

    @Before
    public void setUp() throws Exception {
        dao = new CredentialsDaoImpl(database.getSessionFactory());
        peopleDao = new PeopleDaoImpl(database.getSessionFactory());
        organizationsDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        organization = database.inTransaction(() -> organizationsDao.create(new OrganizationProperties("Test Organization")));
        alice = database.inTransaction(() -> peopleDao.create(organization, new PersonProperties("Alice")));
    }

    @Test
    public void getByProviderAndUsername_existingUser_happyPath() throws Exception {
        database.inTransaction(() -> dao.create(alice, new CredentialsProperties("alice", new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray()))));

        final Credentials actual = database.inTransaction(() -> dao.get(IdentityProvider.PASSWORD, "alice"));
        assertThat(actual.getUsername()).isEqualTo("alice");

        SecretValidator loc = actual.getSecretValidator();
        assertThat(loc).isInstanceOf(PasswordValidator.class);
        assertThat(loc.validate("pw".toCharArray())).isTrue();
    }

    @Test(expected = ObjectNotFoundException.class)
    public void getByProviderAndUsername_missingUser_expectNull() throws Exception {
        dao.get(IdentityProvider.PASSWORD, "trudy");
    }

    @Test
    public void create_duplicateUsername_expectException() throws Exception {
        try {
            dao.create(alice, new CredentialsProperties("alice", new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray())));
        } catch (Exception e) {
            fail("No exception was expected here");
        }
        dao.create(alice, new CredentialsProperties("alice", new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray())));
    }

    @Test
    public void create_newUser_happyPath() throws Exception {
        final Credentials credentials = dao.create(this.alice, new CredentialsProperties("bob", new PasswordValidator(SecretGenerator.PDKDF2, "bobby".toCharArray())));
        assertThat(credentials.getId()).isNotNull();
        assertThat(credentials.getUsername()).isEqualTo("bob");
        assertThat(credentials.getSecretValidator()).isInstanceOf(PasswordValidator.class);
    }

    @Test
    public void getByParent_personWithCredentials_expectOne() throws Exception {
        database.inTransaction(() -> {
            try {
                dao.create(this.alice, new CredentialsProperties("alice1", new PasswordValidator(SecretGenerator.PDKDF2, "pw1".toCharArray())));
                dao.create(this.alice, new CredentialsProperties("alice2", new PasswordValidator(SecretGenerator.PDKDF2, "pw2".toCharArray())));
            } catch (IOException e) {
                fail();
            }
        });
        final List<Credentials> actual = dao.getByParent(alice);
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getUsername()).isEqualTo("alice1");
        assertThat(actual.get(1).getUsername()).isEqualTo("alice2");
    }

    @Test
    public void getByParent_personWithoutCredentials_expectEmptyList() throws Exception {
        final List<Credentials> actual = dao.getByParent(alice);
        assertThat(actual).isEmpty();
    }

    @Test
    public void read_existingCredentials_happyPath() throws Exception {
        final Credentials credentials = database.inTransaction(() -> dao.create(this.alice, new CredentialsProperties("alice2", new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray()))));
        final Credentials actual = dao.read(credentials.getId());
        assertThat(actual.getUsername()).isEqualTo("alice2");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void read_missingCredentials_expectException() throws Exception {
        dao.read(UUID.randomUUID());
    }

//    @Test
//    public void update_changePassword_happyPath() throws Exception {
//        final Credentials credentials = database.inTransaction(() -> dao.create(this.alice, new CredentialsProperties("alice", new PasswordValidator(PasswordValidator.SecretGenerator.PDKDF2, "old password".toCharArray()))));
//        final Credentials actual = dao.update(credentials.getId(), new CredentialsProperties("alice", new PasswordValidator(PasswordValidator.SecretGenerator.PDKDF2, "new password".toCharArray())));
//        assertThat(actual.getUsername()).isEqualTo("alice");
//        assertThat(actual.getSecretValidator().validate("new password".toCharArray())).isTrue();
//    }
//
//    @Test
//    public void update_duplicateUsername_expectException() throws Exception {
//        final Credentials credentials = database.inTransaction(() -> dao.create(this.alice, new CredentialsProperties("alice", new PasswordValidator(PasswordValidator.SecretGenerator.PDKDF2, "old password".toCharArray()))));
//        dao.update(credentials.getId(), new CredentialsProperties("alice", new PasswordValidator(PasswordValidator.SecretGenerator.PDKDF2, "pw".toCharArray())));
//    }
//
//    @Test
//    public void update_missingCredentials_expectException() throws Exception {
//        fail("Not implemented yet");
//    }
//
//    @Test
//    public void delete_existingCredentials_happyPath() throws Exception {
//        fail("Not implemented yet");
//    }
//
//    @Test
//    public void delete_missingCredentials_expectException() throws Exception {
//        fail("Not implemented yet");
//    }
}