package se.devscout.achievements.server.data.dao;

import com.google.common.collect.Sets;
import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.model.*;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AchievementsDaoImplTest {
    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Organization.class)
            .addEntityClass(Person.class)
            .addEntityClass(GroupMembership.class)
            .addEntityClass(Group.class)
            .addEntityClass(Credentials.class)
            .addEntityClass(Achievement.class)
            .addEntityClass(AchievementStep.class)
            .addEntityClass(AchievementStepProgress.class)
            .addEntityClass(StepProgressAuditRecord.class)
            .build();

    private AchievementsDaoImpl dao;
    private PeopleDaoImpl peopleDao;

    @Before
    public void setUp() throws Exception {
        dao = new AchievementsDaoImpl(database.getSessionFactory());
        peopleDao = new PeopleDaoImpl(database.getSessionFactory());
    }

    @Test
    public void findWithProgressForOrganizationAndPerson() throws Exception {
        // Setup: Create organization
        var organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        var org1 = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization 1")));
        var org2 = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization 2")));

        // Setup: Crate people
        var peopleDao = new PeopleDaoImpl(database.getSessionFactory());
        var personAliceWithProgress = database.inTransaction(() -> peopleDao.create(org1, new PersonProperties("Alice", Roles.READER)));
        var personAlanWithProgress = database.inTransaction(() -> peopleDao.create(org1, new PersonProperties("Alan", Roles.READER)));
        var personWithoutProgress = database.inTransaction(() -> peopleDao.create(org1, new PersonProperties("Bob", Roles.READER)));
        var personFromOtherOrgWithProgress = database.inTransaction(() -> peopleDao.create(org2, new PersonProperties("Carol", Roles.READER)));

        // Setup: Create achievements
        var achievementsDao = dao;
        final var achievement1 = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Boil an egg")));
        final var achievement2 = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Cook pasta")));
        final var achievement3 = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Have pizza")));

        // Setup: Create achievement steps
        var stepsDao = new AchievementStepsDaoImpl(database.getSessionFactory());
        var achievement1Step1 = database.inTransaction(() -> stepsDao.create(achievement1, new AchievementStepProperties("Follow the instructions on the package")));
        var achievement1Step2 = database.inTransaction(() -> stepsDao.create(achievement1, new AchievementStepProperties("Clean up afterwards")));
        var achievement2Step1 = database.inTransaction(() -> stepsDao.create(achievement2, new AchievementStepProperties("Pour water into pot")));
        var achievement2Step2 = database.inTransaction(() -> stepsDao.create(achievement2, new AchievementStepProperties("Put pasta into pot")));
        var achievement2Step3 = database.inTransaction(() -> stepsDao.create(achievement2, new AchievementStepProperties("Turn on stove")));
        var achievement2Step4 = database.inTransaction(() -> stepsDao.create(achievement2, new AchievementStepProperties("Wait until it has boiled for the appropriate number of minutes")));
        var achievement2Step5 = database.inTransaction(() -> stepsDao.create(achievement2, new AchievementStepProperties("Use colinder to pour out the water")));
        var achievement2Step6 = database.inTransaction(() -> stepsDao.create(achievement2, new AchievementStepProperties("Serve with bolognese")));
        var achievement3Step1 = database.inTransaction(() -> stepsDao.create(achievement3, new AchievementStepProperties("Order from local pizzeria")));
        var achievement3Step2 = database.inTransaction(() -> stepsDao.create(achievement3, new AchievementStepProperties("Eat and enjoy")));

        // Setup: Create progress records
        var progressDao = new AchievementStepProgressDaoImpl(database.getSessionFactory());
        database.inTransaction(() -> progressDao.set(achievement1Step1, personAliceWithProgress, new AchievementStepProgressProperties(true, "Finally done")));
        database.inTransaction(() -> progressDao.set(achievement1Step2, personAliceWithProgress, new AchievementStepProgressProperties(false, "Still eating the egg")));
        database.inTransaction(() -> progressDao.set(achievement2Step1, personAliceWithProgress, new AchievementStepProgressProperties(true, null)));
        database.inTransaction(() -> progressDao.set(achievement2Step2, personAliceWithProgress, new AchievementStepProgressProperties(true, null)));
        database.inTransaction(() -> progressDao.set(achievement2Step3, personAliceWithProgress, new AchievementStepProgressProperties(true, null)));
        database.inTransaction(() -> progressDao.set(achievement2Step4, personAliceWithProgress, new AchievementStepProgressProperties(true, null)));
        database.inTransaction(() -> progressDao.set(achievement2Step5, personAliceWithProgress, new AchievementStepProgressProperties(true, null)));
        database.inTransaction(() -> progressDao.set(achievement3Step1, personFromOtherOrgWithProgress, new AchievementStepProgressProperties(true, null)));
        database.inTransaction(() -> progressDao.set(achievement3Step1, personAlanWithProgress, new AchievementStepProgressProperties(true, "It is a start")));

        System.out.println("SUT");

        // Testing ORGANIZATONS:

        final var actual1 = achievementsDao.findWithProgressForOrganization(org1);
        assertThat(actual1).hasSize(3);
        assertThat(actual1).containsOnly(achievement1, achievement2, achievement3);

        final var actual2 = achievementsDao.findWithProgressForOrganization(org2);
        assertThat(actual2).hasSize(1);
        assertThat(actual2).containsOnly(achievement3);

        // Testing PEOPLE:

        final var actual3a = database.inTransaction(() -> achievementsDao.findWithProgressForPerson(personAliceWithProgress));
        assertThat(actual3a).hasSize(2);
        assertThat(actual3a).containsOnly(achievement1, achievement2);

        final var actual3b = database.inTransaction(() -> achievementsDao.findWithProgressForPerson(personAlanWithProgress));
        assertThat(actual3b).hasSize(1);
        assertThat(actual3b).containsOnly(achievement3);
        assertThat(actual3b.get(0).getSteps()).hasSize(2);

        final var actual4 = achievementsDao.findWithProgressForPerson(personFromOtherOrgWithProgress);
        assertThat(actual4).hasSize(1);
        assertThat(actual4).containsOnly(achievement3);

        final var actual5 = achievementsDao.findWithProgressForPerson(personWithoutProgress);
        assertThat(actual5).isEmpty();
    }

    @Test
    public void get_happyPath() throws Exception {
        var aliceUuid = database.inTransaction(() -> dao.create(new AchievementProperties("Cook Pasta"))).getId();
        final var actual = dao.read(aliceUuid);
        assertThat(actual.getName()).isEqualTo("Cook Pasta");
    }

    @Test
    public void find_singleWord_happyPath() throws Exception {
        var fire = database.inTransaction(() -> dao.create(new AchievementProperties("Make Fire"))).getId();
        var raft = database.inTransaction(() -> dao.create(new AchievementProperties("Make Raft"))).getId();
        var treasure = database.inTransaction(() -> dao.create(new AchievementProperties("Find Treasure"))).getId();
        final var actual = dao.find("Make");
        var returnedUuids = actual.stream().map(Achievement::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(fire, raft);
    }

    @Test
    public void find_tag_happyPath() throws Exception {
        var fire = database.inTransaction(() -> dao.create(new AchievementProperties("Make Fire", "description", Sets.newHashSet("matches", "wood")))).getId();
        var raft = database.inTransaction(() -> dao.create(new AchievementProperties("Make Raft", "description", Sets.newHashSet("water", "boat")))).getId();
        final var actual = dao.find("boat");
        var returnedUuids = actual.stream().map(Achievement::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(raft);
    }

    @Test
    public void find_twoWords_happyPath() {
        var fire = database.inTransaction(() -> dao.create(new AchievementProperties("Make Fire"))).getId();
        var raft = database.inTransaction(() -> dao.create(new AchievementProperties("Make Raft"))).getId();
        var treasure = database.inTransaction(() -> dao.create(new AchievementProperties("Find Treasure"))).getId();
        final var actual = dao.find("Make Raft");
        var returnedUuids = actual.stream().map(Achievement::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(raft);
    }

    @Test
    public void find_quotedSentence_happyPath() {
        var fire = database.inTransaction(() -> dao.create(new AchievementProperties("aab ccd eef"))).getId();
        var raft = database.inTransaction(() -> dao.create(new AchievementProperties("aab iij kkl"))).getId();
        var treasure = database.inTransaction(() -> dao.create(new AchievementProperties("mmn oop qqr"))).getId();
        final var actual = dao.find("\"aab ccd\"");
        var returnedUuids = actual.stream().map(Achievement::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(fire);
    }

    @Test
    public void find_caseInsensitive_happyPath() {
        var fire1 = database.inTransaction(() -> dao.create(new AchievementProperties("make fire"))).getId();
        var fire2 = database.inTransaction(() -> dao.create(new AchievementProperties("MAKE FIRE"))).getId();
        var treasure = database.inTransaction(() -> dao.create(new AchievementProperties("Find Treasure"))).getId();
        final var actual = dao.find("mAkE FiRe");
        var returnedUuids = actual.stream().map(Achievement::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(fire1, fire2);
    }

    @Test
    public void readAll_achievementsExists_happyPath() throws Exception {
        var fire = database.inTransaction(() -> dao.create(new AchievementProperties("Make Fire"))).getId();
        var raft = database.inTransaction(() -> dao.create(new AchievementProperties("Make Raft"))).getId();
        var treasure = database.inTransaction(() -> dao.create(new AchievementProperties("Find Treasure"))).getId();
        final var actual = dao.readAll();
        var returnedUuids = actual.stream().map(Achievement::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(fire, raft, treasure);
    }

    @Test
    public void readAll_emptyDatabase_happyPath() throws Exception {
        final var actual = dao.readAll();
        assertThat(actual).isNotNull();
        assertThat(actual).isEmpty();
    }

    @Test
    public void find_noCondition_expectException() throws Exception {
        for (var name : new String[]{null, "", "\t", "     ", "\n"}) {
            try {
                dao.find(name);
                fail("Expected IllegalArgumentException when using condition '" + name + "'.");
            } catch (IllegalArgumentException e) {
                // Expected.
            }
        }
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_notFound() throws Exception {
        dao.read(UUID.randomUUID());
    }

    @Test
    public void delete_happyPath() throws Exception {
        var id = database.inTransaction(() -> dao.create(new AchievementProperties("Something"))).getId();
        database.inTransaction(() -> {
            try {
                dao.delete(id);
            } catch (ObjectNotFoundException e) {
                fail();
            }
        });
        database.inTransaction(() -> {
            try {
                dao.read(id);
                fail();
            } catch (ObjectNotFoundException e) {
            }
        });
    }

    @Test(expected = ObjectNotFoundException.class)
    public void delete_notFound() throws Exception {
        dao.delete(UUID.randomUUID());
    }

    @Test
    public void create_happyPath() throws Exception {
        final var result = database.inTransaction(() -> dao.create(new AchievementProperties("Boil Egg")));
        final var actual = database.inTransaction(() -> dao.read(result.getId()));
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getName()).isEqualTo("Boil Egg");
    }

    @Test
    public void update_simpleAchievement_happyPath() throws Exception {
        var objectUuid = database.inTransaction(() -> dao.create(new AchievementProperties("Cook Pasta"))).getId();

        database.inTransaction(() -> dao.update(objectUuid, new AchievementProperties("Cook Spagetti")));

        final var actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Cook Spagetti");
    }

    @Test
    public void awardedTo_happyPath() throws Exception {
        // Setup: Create achievements
        final var achievementPasta = database.inTransaction(() -> dao.create(new AchievementProperties("Cook Pasta")));
        final var achievementEgg = database.inTransaction(() -> dao.create(new AchievementProperties("Cook Egg")));

        // Setup: Create organization
        var organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        var org = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization")));

        // Setup: Crate people
        var personAlice = database.inTransaction(() -> peopleDao.create(org, new PersonProperties("Alice", Roles.READER)));
        var personBob = database.inTransaction(() -> peopleDao.create(org, new PersonProperties("Bob", Roles.READER)));

        // TEST addAwardedTo
        database.inTransaction(() -> dao.addAwardedTo(achievementEgg, personAlice));
        database.inTransaction(() -> dao.addAwardedTo(achievementEgg, personBob));
        database.inTransaction(() -> dao.addAwardedTo(achievementPasta, personAlice));

        verifyIsAwardedTo(achievementEgg.getId(), personAlice, personBob);
        verifyIsAwardedTo(achievementPasta.getId(), personAlice);
        verifyHasBeenAwarded(personAlice.getId(), achievementEgg, achievementPasta);
        verifyHasBeenAwarded(personBob.getId(), achievementEgg);

        // TEST addAwardedTo
        database.inTransaction(() -> dao.addAwardedTo(achievementPasta, personBob));

        verifyIsAwardedTo(achievementEgg.getId(), personAlice, personBob);
        verifyIsAwardedTo(achievementPasta.getId(), personAlice, personBob);
        verifyHasBeenAwarded(personAlice.getId(), achievementEgg, achievementPasta);
        verifyHasBeenAwarded(personBob.getId(), achievementEgg, achievementPasta);

        // TEST removeAwardedTo
        database.inTransaction(() -> dao.removeAwardedTo(achievementPasta, personAlice));
        database.inTransaction(() -> dao.removeAwardedTo(achievementPasta, personBob));
        database.inTransaction(() -> dao.removeAwardedTo(achievementEgg, personBob));

        verifyIsAwardedTo(achievementEgg.getId(), personAlice);
        verifyIsAwardedTo(achievementPasta.getId());
        verifyHasBeenAwarded(personAlice.getId(), achievementEgg);
        verifyHasBeenAwarded(personBob.getId());
    }

    private void verifyHasBeenAwarded(int id, Achievement... achievements) {
        final var actual = database.inTransaction(() -> peopleDao.read(id));
        assertThat(actual.getId()).isEqualTo(id);
        assertThat(actual.getAwards()).containsOnly(achievements);
    }

    private void verifyIsAwardedTo(UUID id, Person... people) {
        final var actual = database.inTransaction(() -> dao.read(id));
        assertThat(actual.getId()).isEqualTo(id);
        assertThat(actual.getAwardedTo()).containsOnly(people);
    }

    @Test
    public void update_complexAchievement_happyPath() throws Exception {
        var objectUuid = database.inTransaction(() -> {
            final var initialProperties = new AchievementProperties(
                    "Cook Pasta",
                    Sets.newHashSet("italian", "simple", "tasty"));
            return dao.create(initialProperties);
        }).getId();

        database.inTransaction(() -> {
            final var updatedProperties = new AchievementProperties(
                    "Cook Spagetti",
                    Sets.newHashSet("italian", "quick"));
            return dao.update(objectUuid, updatedProperties);
        });

        final var actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Cook Spagetti");
        assertThat(actual.getTags()).containsExactlyInAnyOrder("italian", "quick");
    }
}