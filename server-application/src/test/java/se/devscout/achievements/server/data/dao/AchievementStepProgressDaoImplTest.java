package se.devscout.achievements.server.data.dao;

import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.model.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AchievementStepProgressDaoImplTest {

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

    private AchievementStepProgressDaoImpl dao;
    private Person person;
    private Person person2;
    private AchievementStep achievementStep;
    private Person personWithoutProgress;
    private PeopleDaoImpl peopleDao;
    private AchievementsDaoImpl achievementsDao;
    private AchievementStepsDaoImpl stepsDao;

    @Before
    public void setUp() throws Exception {
        dao = new AchievementStepProgressDaoImpl(database.getSessionFactory());
        var organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        var organization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization")));
        peopleDao = new PeopleDaoImpl(database.getSessionFactory());
        achievementsDao = new AchievementsDaoImpl(database.getSessionFactory());
        stepsDao = new AchievementStepsDaoImpl(database.getSessionFactory());

        person = peopleDao.create(organization, new PersonProperties("Alice", Roles.READER));
        person2 = peopleDao.create(organization, new PersonProperties("Alice", Roles.READER));
        personWithoutProgress = peopleDao.create(organization, new PersonProperties("Bob", Roles.READER));

        final var achievement1 = achievementsDao.create(new AchievementProperties("Boil an egg"));
        achievementStep = stepsDao.create(achievement1, new AchievementStepProperties("Follow the instructions on the package"));

    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_noProgress_happyPath() throws Exception {
        dao.get(achievementStep, personWithoutProgress);
    }

    @Test
    public void getAll_noProgress_happyPath() throws Exception {
        final var achievement = database.inTransaction(() -> achievementsDao.create(new AchievementProperties("Make a sandwich")));
        final var step1 = database.inTransaction(() -> stepsDao.create(achievement, new AchievementStepProperties("Get the bread")));
        final var step2 = database.inTransaction(() -> stepsDao.create(achievement, new AchievementStepProperties("Spread butter on it")));
        database.inTransaction(() -> {
            dao.set(step1, person, new AchievementStepProgressProperties(true, "Note 1"));
            dao.set(step1, person2, new AchievementStepProgressProperties(true, "Note 2"));
            dao.set(step2, person2, new AchievementStepProgressProperties(true, "Note 3"));
        });

        final var progressList = dao.get(achievement);
        assertThat(progressList).hasSize(3);
    }

    @Test
    public void setAndGet_newProgress_happyPath() throws Exception {
        final var actual = dao.set(achievementStep, person, new AchievementStepProgressProperties(true, "The Note"));
        assertThat(actual).isNotNull();
        assertThat(actual.isCompleted()).isTrue();
        assertThat(actual.getNote()).isEqualTo("The Note");

        final var actualAfter = database.inTransaction(() -> dao.get(achievementStep, person));
        assertThat(actualAfter).isNotNull();
    }

    @Test
    public void setAndSet_existingProgress_happyPath() throws Exception {
        final var existing = database.inTransaction(() -> dao.set(achievementStep, person, new AchievementStepProgressProperties(false, "The Note")));

        assertThat(existing.getId()).isNotEqualTo(0);
        assertThat(existing.isCompleted()).isFalse();
        assertThat(existing.getNote()).isEqualTo("The Note");

        final var actual = dao.set(achievementStep, person, new AchievementStepProgressProperties(true, "A Note"));
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(existing.getId());
        assertThat(actual.isCompleted()).isTrue();
        assertThat(actual.getNote()).isEqualTo("A Note");
    }

    @Test
    public void setAndUnset_happyPath() throws Exception {
        database.inTransaction(() -> {
            dao.set(achievementStep, person, new AchievementStepProgressProperties(true, "The Note"));
        });
        database.inTransaction(() -> {
            try {
                dao.unset(achievementStep, person);
            } catch (ObjectNotFoundException e) {
                fail();
            }
        });
        database.inTransaction(() -> {
            try {
                dao.get(achievementStep, person);
                fail();
            } catch (ObjectNotFoundException e) {
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void unset_badInput() throws Exception {
        dao.unset(null, null);
    }
}