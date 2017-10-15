package se.devscout.achievements.server.data.dao;

import com.google.common.collect.Sets;
import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;
import se.devscout.achievements.server.data.model.AchievementStep;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AchievementsDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Achievement.class)
            .addEntityClass(AchievementStep.class)
            .build();

    private AchievementsDaoImpl dao;

    @Before
    public void setUp() throws Exception {
        dao = new AchievementsDaoImpl(database.getSessionFactory());
    }

    @Test
    public void get_happyPath() throws Exception {
        UUID aliceUuid = database.inTransaction(() -> dao.create(new AchievementProperties("Cook Pasta"))).getId();
        final Achievement actual = dao.read(aliceUuid);
        assertThat(actual.getName()).isEqualTo("Cook Pasta");
    }

    @Test
    public void find_happyPath() throws Exception {
        UUID fire = database.inTransaction(() -> dao.create(new AchievementProperties("Make Fire"))).getId();
        UUID raft = database.inTransaction(() -> dao.create(new AchievementProperties("Make Raft"))).getId();
        UUID treasure = database.inTransaction(() -> dao.create(new AchievementProperties("Find Treasure"))).getId();
        final List<Achievement> actual = dao.find("Make");
        List<UUID> returnedUuids = actual.stream().map(Achievement::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(fire, raft);
    }

    @Test
    public void find_noCondition_expectException() throws Exception {
        for (String name : new String[]{null, "", "\t", "     ", "\n"}) {
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
        UUID id = database.inTransaction(() -> dao.create(new AchievementProperties("Something"))).getId();
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
        final Achievement result = database.inTransaction(() -> dao.create(new AchievementProperties("Boil Egg")));
        final Achievement actual = database.inTransaction(() -> dao.read(result.getId()));
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getName()).isEqualTo("Boil Egg");
    }

    @Test
    public void update_simpleAchievement_happyPath() throws Exception {
        UUID objectUuid = database.inTransaction(() -> dao.create(new AchievementProperties("Cook Pasta"))).getId();

        database.inTransaction(() -> dao.update(objectUuid, new AchievementProperties("Cook Spagetti")));

        final Achievement actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Cook Spagetti");
    }

    @Test
    public void update_complexAchievement_happyPath() throws Exception {
        UUID objectUuid = database.inTransaction(() -> {
            final AchievementProperties initialProperties = new AchievementProperties(
                    "Cook Pasta",
                    Sets.newHashSet("italian", "simple", "tasty"));
            return dao.create(initialProperties);
        }).getId();

        database.inTransaction(() -> {
            final AchievementProperties updatedProperties = new AchievementProperties(
                    "Cook Spagetti",
                    Sets.newHashSet("italian", "quick"));
            return dao.update(objectUuid, updatedProperties);
        });

        final Achievement actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Cook Spagetti");
        assertThat(actual.getTags()).containsExactlyInAnyOrder("italian", "quick");
    }
}