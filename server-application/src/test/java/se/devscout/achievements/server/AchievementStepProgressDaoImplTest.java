package se.devscout.achievements.server;

import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AchievementStepProgressDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Organization.class)
            .addEntityClass(Person.class)
            .addEntityClass(Achievement.class)
            .addEntityClass(AchievementStep.class)
            .addEntityClass(AchievementStepProgress.class)
            .build();

    private AchievementStepProgressDaoImpl dao;
    private Person person;
    private AchievementStep achievementStep;
    private Person personWithoutProgress;

    @Before
    public void setUp() throws Exception {
        dao = new AchievementStepProgressDaoImpl(database.getSessionFactory());
        OrganizationsDaoImpl organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        Organization organization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization")));
        final PeopleDaoImpl peopleDao = new PeopleDaoImpl(database.getSessionFactory());
        person = peopleDao.create(organization, new PersonProperties("Alice"));
        personWithoutProgress = peopleDao.create(organization, new PersonProperties("Bob"));
        final Achievement achievement = new AchievementsDaoImpl(database.getSessionFactory()).create(new AchievementProperties("Boil an egg"));
        achievementStep = new AchievementStepsDaoImpl(database.getSessionFactory()).create(achievement, new AchievementStepProperties("Follow the instructions on the package"));
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_noProgress_happyPath() throws Exception {
        dao.get(achievementStep, personWithoutProgress);
    }

    @Test
    public void setAndGet_newProgress_happyPath() throws Exception {
        final AchievementStepProgress actual = dao.set(achievementStep, person, new AchievementStepProgressProperties(true, "The Note"));
        assertThat(actual).isNotNull();
        assertThat(actual.isCompleted()).isTrue();
        assertThat(actual.getNote()).isEqualTo("The Note");

        final AchievementStepProgress actualAfter = database.inTransaction(() -> dao.get(achievementStep, person));
        assertThat(actualAfter).isNotNull();
    }

    @Test
    public void setAndSet_existingProgress_happyPath() throws Exception {
        final AchievementStepProgress existing = database.inTransaction(() -> dao.set(achievementStep, person, new AchievementStepProgressProperties(false, "The Note")));

        assertThat(existing.getId()).isNotEqualTo(0);
        assertThat(existing.isCompleted()).isFalse();
        assertThat(existing.getNote()).isEqualTo("The Note");

        final AchievementStepProgress actual = dao.set(achievementStep, person, new AchievementStepProgressProperties(true, "A Note"));
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(existing.getId());
        assertThat(actual.isCompleted()).isTrue();
        assertThat(actual.getNote()).isEqualTo("A Note");
    }

    @Test
    public void setAndUnset_happyPath() throws Exception {
        database.inTransaction(() -> {
            try {
                dao.set(achievementStep, person, new AchievementStepProgressProperties(true, "The Note"));
            } catch (ObjectNotFoundException e) {
                fail();
            }
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