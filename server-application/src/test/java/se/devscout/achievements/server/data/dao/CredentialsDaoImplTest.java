package se.devscout.achievements.server.data.dao;

import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.model.*;

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
        //TODO: Shared set-up code:
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray());
        database.inTransaction(() -> dao.create(alice, new CredentialsProperties("alice", passwordValidator.getIdentityProvider(), passwordValidator.getSecret())));

        final Credentials actual = database.inTransaction(() -> dao.get(IdentityProvider.PASSWORD, "alice"));
        assertThat(actual.getUsername()).isEqualTo("alice");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void getByProviderAndUsername_missingUser_expectNull() throws Exception {
        dao.get(IdentityProvider.PASSWORD, "trudy");
    }

    @Test
    public void create_duplicateUsername_expectException() throws Exception {
        try {
            final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray());
            dao.create(alice, new CredentialsProperties("alice", passwordValidator.getIdentityProvider(), passwordValidator.getSecret()));
        } catch (Exception e) {
            fail("No exception was expected here");
        }
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray());
        dao.create(alice, new CredentialsProperties("alice", passwordValidator.getIdentityProvider(), passwordValidator.getSecret()));
    }

    @Test
    public void create_newUser_happyPath() throws Exception {
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "bobby".toCharArray());
        final Credentials credentials = dao.create(this.alice, new CredentialsProperties("bob", passwordValidator.getIdentityProvider(), passwordValidator.getSecret()));
        assertThat(credentials.getId()).isNotNull();
        assertThat(credentials.getUsername()).isEqualTo("bob");
    }

    @Test
    public void getByParent_personWithCredentials_expectOne() throws Exception {
        database.inTransaction(() -> {
            final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "pw1".toCharArray());
            dao.create(this.alice, new CredentialsProperties("alice1", passwordValidator.getIdentityProvider(), passwordValidator.getSecret()));
            final PasswordValidator passwordValidator1 = new PasswordValidator(SecretGenerator.PDKDF2, "pw2".toCharArray());
            dao.create(this.alice, new CredentialsProperties("alice2", passwordValidator1.getIdentityProvider(), passwordValidator1.getSecret()));
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
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray());
        final Credentials credentials = database.inTransaction(() -> dao.create(this.alice, new CredentialsProperties("alice2", passwordValidator.getIdentityProvider(), passwordValidator.getSecret())));
        final Credentials actual = dao.read(credentials.getId());
        assertThat(actual.getUsername()).isEqualTo("alice2");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void read_missingCredentials_expectException() throws Exception {
        dao.read(UUID.randomUUID());
    }
}