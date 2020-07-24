package se.devscout.achievements.server.data.dao;

import com.google.common.collect.Sets;
import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.model.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PeopleDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Organization.class)
            .addEntityClass(Achievement.class)
            .addEntityClass(AchievementStep.class)
            .addEntityClass(AchievementStepProgress.class)
            .addEntityClass(StepProgressAuditRecord.class)
            .addEntityClass(Person.class)
            .addEntityClass(GroupMembership.class)
            .addEntityClass(Group.class)
            .addEntityClass(Credentials.class)
            .build();

    private PeopleDaoImpl dao;
    private AchievementsDaoImpl achievementsDao;

    private Organization testOrganization;
    private Organization otherOrganization;

    @Before
    public void setUp() throws Exception {
        dao = new PeopleDaoImpl(database.getSessionFactory());
        achievementsDao = new AchievementsDaoImpl(database.getSessionFactory());

        var organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        testOrganization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization")));
        otherOrganization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Other Organization")));
    }

    @Test
    public void get_happyPath() throws Exception {
        var aliceUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", Roles.READER))).getId();
        final var actual = dao.read(aliceUuid);
        assertThat(actual.getName()).isEqualTo("Alice");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_notFound() throws Exception {
        dao.read(-1);
    }

    @Test
    public void getByOrganization_happyPath() throws Exception {
        var aliceUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", Roles.READER))).getId();
        var amandaUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Amanda", Roles.READER))).getId();
        var bobUuid = database.inTransaction(() -> dao.create(otherOrganization, new PersonProperties("Bob", Roles.READER))).getId();

        final var actualA = dao.getByParent(testOrganization);
        assertThat(actualA.stream().map(Person::getId).collect(Collectors.toList())).containsExactlyInAnyOrder(aliceUuid, amandaUuid);

        final var actualB = dao.getByParent(otherOrganization);
        assertThat(actualB.stream().map(Person::getId).collect(Collectors.toList())).containsExactlyInAnyOrder(bobUuid);
    }

    @Test
    public void getByOrganization_incorrectId_expectEmptyList() throws Exception {
        final var actual = dao.getByParent(null);
        assertThat(actual).isEmpty();
    }

    @Test
    public void getByEmail_addressExists_happyPath() throws Exception {
        database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", "alice@example.com", Collections.emptySet(), "alice", Roles.READER))).getId();

        final var actual = dao.getByEmail("alice@example.com");
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    public void getByEmail_caseInsensitiveSearch_happyPath() throws Exception {
        database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", "ALICE@example.com", Collections.emptySet(), "alice", Roles.READER))).getId();

        final var actual = dao.getByEmail("alice@EXAMPLE.com");
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getEmail()).isEqualTo("ALICE@example.com");
    }

    @Test
    public void getByEmail_addressIsMissing_happyPath() throws Exception {
        database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", "alice@example.com", Collections.emptySet(), "alice", Roles.READER))).getId();

        final var actual = dao.getByEmail("bob@example.com");
        assertThat(actual).hasSize(0);
    }

    @Test
    public void delete_happyPath() throws Exception {
        final var personAliceWithProgress = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", Roles.READER)));

        // Setup: Create achievements
        var achievementsDao = new AchievementsDaoImpl(database.getSessionFactory());
        final var achievement1 = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Boil an egg")));

        // Setup: Create achievement steps
        var stepsDao = new AchievementStepsDaoImpl(database.getSessionFactory());
        var achievement1Step1 = database.inTransaction(() -> stepsDao.create(achievement1, new AchievementStepProperties("Follow the instructions on the package")));
        var achievement1Step2 = database.inTransaction(() -> stepsDao.create(achievement1, new AchievementStepProperties("Clean up afterwards")));

        // Setup: Create progress records
        var auditingDao = new AuditingDaoImpl(database.getSessionFactory());
        var progressDao = new AchievementStepProgressDaoImpl(database.getSessionFactory());
        database.inTransaction(() -> {
            progressDao.set(achievement1Step1, personAliceWithProgress, new AchievementStepProgressProperties(true, "Finally done"));
            auditingDao.create(UUID.randomUUID(), 1, achievement1Step1.getId(), personAliceWithProgress.getId(), null, "PUT", 200);
            return null;
        });
        database.inTransaction(() -> progressDao.set(achievement1Step2, personAliceWithProgress, new AchievementStepProgressProperties(false, "Still eating the egg")));

        database.getSessionFactory().getCurrentSession().clear(); // TODO: Figure out of we need this hack because of missing "production code" or if the test setup is somehow incorrect.

        database.inTransaction(() -> {
            try {
                dao.delete(personAliceWithProgress.getId());
            } catch (ObjectNotFoundException e) {
                fail();
            }
        });
        database.inTransaction(() -> {
            try {
                dao.read(personAliceWithProgress.getId());
                fail();
            } catch (ObjectNotFoundException e) {
            }
        });
    }

    @Test(expected = ObjectNotFoundException.class)
    public void delete_notFound() throws Exception {
        dao.delete(-1);
    }

    @Test
    public void create_happyPath() throws Exception {
        final var result = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Carol", "carol@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), null, Roles.READER)));
        final var actual = database.inTransaction(() -> dao.read(result.getId()));
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getName()).isEqualTo("Carol");
        assertThat(actual.getEmail()).isEqualTo("carol@example.com");

        assertThat(actual.getAttributes()).hasSize(2);
        assertThat(actual.getAttributes()).contains(new PersonAttribute("favourite_colour", "green"));
        assertThat(actual.getAttributes()).contains(new PersonAttribute("role", "administrator"));
    }

    @Test
    public void create_sameNameAsExistingPerson_happyPath() throws Exception {
        final var c1 = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Carol", "carol1@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), null, Roles.READER)));
        final var c2 = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Carol", "carol2@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), null, Roles.READER)));
        assertThat(c1.getId()).isNotEqualTo(c2.getId());
    }

    @Test(expected = DuplicateCustomIdentifier.class)
    public void create_duplicateCustomId_notAllowed() throws Exception {
        try {
            dao.create(testOrganization, new PersonProperties("Carol1", "carol1@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), "carol", Roles.READER));
        } catch (Exception e) {
            fail("Exception was not expected");
        }
        dao.create(testOrganization, new PersonProperties("Carol2", "carol2@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), "carol", Roles.READER));
    }

    @Test
    public void update_personWithoutAttributes_happyPath() throws Exception {
        var objectUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Belinda", Roles.READER))).getId();

        database.inTransaction(() -> dao.update(objectUuid, new PersonProperties("Becky", Roles.READER)));

        final var actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Becky");
        assertThat(actual.getAttributes()).isEmpty();
    }

    @Test
    public void awards_happyPath() throws Exception {
        // Setup: Create achievements
        var achievementsDao = new AchievementsDaoImpl(database.getSessionFactory());
        final var achievement1 = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Boil an egg")));
        final var achievement2 = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Make an omelette")));

        // Setup: Create people
        final var bill = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Bill", Roles.READER)));
        final var belinda = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Belinda", Roles.READER)));

        // TEST: Award two achievements to Belinda
        database.inTransaction(() -> dao.addAwardFor(belinda, achievement1));
        database.inTransaction(() -> dao.addAwardFor(belinda, achievement2));

        verifyIsAwardedTo(achievement1.getId(), belinda);
        verifyIsAwardedTo(achievement2.getId(), belinda);
        verifyHasBeenAwarded(bill.getId());
        verifyHasBeenAwarded(belinda.getId(), achievement1, achievement2);

        // TEST: Award one achievement to Bill
        database.inTransaction(() -> dao.addAwardFor(bill, achievement1));

        verifyIsAwardedTo(achievement1.getId(), belinda, bill);
        verifyIsAwardedTo(achievement2.getId(), belinda);
        verifyHasBeenAwarded(bill.getId(), achievement1);
        verifyHasBeenAwarded(belinda.getId(), achievement1, achievement2);

        // TEST: Remove one of Belinda's awards
        database.inTransaction(() -> dao.removeAwardFor(belinda, achievement1));

        verifyIsAwardedTo(achievement1.getId(), bill);
        verifyIsAwardedTo(achievement2.getId(), belinda);
        verifyHasBeenAwarded(bill.getId(), achievement1);
        verifyHasBeenAwarded(belinda.getId(), achievement2);
    }

    private void verifyHasBeenAwarded(int id, Achievement... achievements) {
        final var actual = database.inTransaction(() -> dao.read(id));
        assertThat(actual.getId()).isEqualTo(id);
        assertThat(actual.getAwards()).containsOnly(achievements);
    }

    private void verifyIsAwardedTo(UUID id, Person... people) {
        final var actual = database.inTransaction(() -> achievementsDao.read(id));
        assertThat(actual.getId()).isEqualTo(id);
        assertThat(actual.getAwardedTo()).containsOnly(people);
    }

    @Test
    public void getByAwardedAchievement_differentOrganizations() throws Exception {
        // Setup: Create achievements
        final var achievementPasta = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Cook Pasta")));
        final var achievementEgg = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Cook Egg")));

        // Setup: Create organization
        var organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        var org1 = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization A")));
        var org2 = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization B")));

        // Setup: Crate people
        var personAlice = database.inTransaction(() -> dao.create(org1, new PersonProperties("Alice", Roles.READER)));
        var personBob = database.inTransaction(() -> dao.create(org1, new PersonProperties("Bob", Roles.READER)));
        var personCarol = database.inTransaction(() -> dao.create(org2, new PersonProperties("Carol", Roles.READER)));

        // Setup: Award people different achievements
        database.inTransaction(() -> dao.addAwardFor(personAlice, achievementEgg));
        database.inTransaction(() -> dao.addAwardFor(personBob, achievementEgg));
        database.inTransaction(() -> dao.addAwardFor(personCarol, achievementEgg));
        database.inTransaction(() -> dao.addAwardFor(personAlice, achievementPasta));
        database.inTransaction(() -> dao.addAwardFor(personCarol, achievementPasta));

        // TEST getByAwardedAchievement
        assertThat(database.inTransaction(() -> dao.getByAwardedAchievement(org1, achievementEgg))).containsExactly(personAlice, personBob);
        assertThat(database.inTransaction(() -> dao.getByAwardedAchievement(org2, achievementEgg))).containsExactly(personCarol);
        assertThat(database.inTransaction(() -> dao.getByAwardedAchievement(org1, achievementPasta))).containsExactly(personAlice);
        assertThat(database.inTransaction(() -> dao.getByAwardedAchievement(org2, achievementPasta))).containsExactly(personCarol);
    }

    @Test(expected = DuplicateCustomIdentifier.class)
    public void update_duplicateCustomId_notAllowed() throws Exception {
        Integer objectUuid = null;

        try {
            database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Belinda Cooper", "belinda_id", Roles.READER))).getId();
            objectUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Belinda Jones", Roles.READER))).getId();
        } catch (Exception e) {
            fail("Exception was not expected");
        }

        dao.update(objectUuid, new PersonProperties("Belinda Jones", "belinda_id", Roles.READER));
    }

    @Test
    public void update_personWithAttributes_happyPath() throws Exception {
        update_personWithAttributes_happyPath(null, "dave");
    }

    @Test
    public void update_personWithUnchangedCustomId_happyPath() throws Exception {
        update_personWithAttributes_happyPath("dave", "dave");
    }

    private void update_personWithAttributes_happyPath(String initialCustomIdentifier, String updatedCustomIdentifier) {
        var objectUuid = database.inTransaction(() -> {
            final var initialProperties = new PersonProperties("Dave", "dave@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "orange"), new PersonAttribute("role", "administrator")), initialCustomIdentifier, Roles.READER);
            return dao.create(testOrganization, initialProperties);
        }).getId();

        database.inTransaction(() -> {
            final var updatedProperties = new PersonProperties("David", null, Sets.newHashSet(new PersonAttribute("favourite_colour", "blue"), new PersonAttribute("title", "administrator")), updatedCustomIdentifier, Roles.READER);
            return dao.update(objectUuid, updatedProperties);
        });

        final var actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("David");
        assertThat(actual.getEmail()).isNull();
        assertThat(actual.getCustomIdentifier()).isEqualTo(updatedCustomIdentifier);
        assertThat(actual.getAttributes()).hasSize(2);
        assertThat(actual.getAttributes()).contains(new PersonAttribute("favourite_colour", "blue"));
        assertThat(actual.getAttributes()).contains(new PersonAttribute("title", "administrator"));
    }
}