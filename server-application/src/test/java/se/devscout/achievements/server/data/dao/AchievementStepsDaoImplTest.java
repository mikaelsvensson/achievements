package se.devscout.achievements.server.data.dao;

import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.model.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AchievementStepsDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Achievement.class)
            .addEntityClass(AchievementStep.class)
            .addEntityClass(AchievementStepProgress.class)
            .addEntityClass(Person.class)
            .addEntityClass(GroupMembership.class)
            .addEntityClass(Group.class)
            .addEntityClass(Organization.class)
            .addEntityClass(Credentials.class)
            .addEntityClass(StepProgressAuditRecord.class)
            .build();

    private AchievementStepsDao dao;
    private AchievementsDao achievementsDao;
    private Achievement achievement;

    @Before
    public void setUp() throws Exception {
        dao = new AchievementStepsDaoImpl(database.getSessionFactory());
        achievementsDao = new AchievementsDaoImpl(database.getSessionFactory());
        achievement = achievementsDao.create(new AchievementProperties("Cook Pasta"));
    }

    @Test
    public void get_happyPath() throws Exception {
        Integer stepId = database.inTransaction(() -> dao.create(achievement, new AchievementStepProperties("Follow instrucions on package"))).getId();
        final AchievementStep actual = dao.read(stepId);
        assertThat(actual.getId()).isEqualTo(stepId);
        assertThat(actual.getDescription()).isEqualTo("Follow instrucions on package");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_notFound() throws Exception {
        dao.read(-1);
    }

    @Test
    public void delete_happyPath() throws Exception {
        Integer stepId = database.inTransaction(() -> dao.create(achievement, new AchievementStepProperties("Serve with ketchup"))).getId();
        database.inTransaction(() -> {
            try {
                dao.delete(stepId);
            } catch (ObjectNotFoundException e) {
                fail();
            }
        });
        database.inTransaction(() -> {
            try {
                dao.read(stepId);
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
        Achievement achievement2 = achievementsDao.create(new AchievementProperties("Buy Ingredients"));
        Integer step1Id = database.inTransaction(() -> dao.create(achievement, new AchievementStepProperties(achievement2))).getId();
        Integer step2Id = database.inTransaction(() -> dao.create(achievement, new AchievementStepProperties("Follow instrucions on package"))).getId();

        final AchievementStep actual1 = database.inTransaction(() -> dao.read(step1Id));
        assertThat(actual1.getId()).isNotNull();
        assertThat(actual1.getAchievement().getId()).isEqualTo(achievement.getId());
        assertThat(actual1.getPrerequisiteAchievement().getId()).isEqualTo(achievement2.getId());
        assertThat(actual1.getDescription()).isNull();

        final AchievementStep actual2 = database.inTransaction(() -> dao.read(step2Id));
        assertThat(actual2.getId()).isNotNull();
        assertThat(actual2.getAchievement().getId()).isEqualTo(achievement.getId());
        assertThat(actual2.getPrerequisiteAchievement()).isNull();
        assertThat(actual2.getDescription()).isEqualTo("Follow instrucions on package");
    }

    @Test
    public void getByParent_happyPath() throws Exception {
        Achievement achievement2 = achievementsDao.create(new AchievementProperties("Buy Ingredients"));
        Integer step1Id = database.inTransaction(() -> dao.create(achievement, new AchievementStepProperties(achievement2))).getId();
        Integer step2Id = database.inTransaction(() -> dao.create(achievement, new AchievementStepProperties("Follow instrucions on package"))).getId();

        final List<AchievementStep> actual = database.inTransaction(() -> dao.getByParent(achievement));

        assertThat(actual).hasSize(2);

        assertThat(actual.get(0).getId()).isEqualTo(step1Id);
        assertThat(actual.get(0).getDescription()).isNull();
        assertThat(actual.get(0).getPrerequisiteAchievement()).isNotNull();

        assertThat(actual.get(1).getId()).isEqualTo(step2Id);
        assertThat(actual.get(1).getDescription()).isEqualTo("Follow instrucions on package");
        assertThat(actual.get(1).getPrerequisiteAchievement()).isNull();
    }

    @Test
    public void update_changeReferenceToDescription_happyPath() throws Exception {
        Achievement achievement2 = achievementsDao.create(new AchievementProperties("Buy Ingredients"));
        Integer step1Id = database.inTransaction(() -> dao.create(achievement, new AchievementStepProperties(achievement2))).getId();

        database.inTransaction(() -> dao.update(step1Id, new AchievementStepProperties("Get food"))).getId();

        final AchievementStep actual = database.inTransaction(() -> dao.read(step1Id));
        assertThat(actual.getId()).isEqualTo(step1Id);
        assertThat(actual.getPrerequisiteAchievement()).isNull();
        assertThat(actual.getDescription()).isEqualTo("Get food");
    }
}