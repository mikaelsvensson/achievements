package se.devscout.achievements.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.dao.AchievementsDaoImpl;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.*;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AchievementsDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Achievement.class)
            .addEntityClass(AchievementStep.class)
            .addEntityClass(AchievementStandardStep.class)
            .addEntityClass(AchievementReferenceStep.class)
            .build();

    private AchievementsDaoImpl dao;

    @Before
    public void setUp() throws Exception {
        dao = new AchievementsDaoImpl(database.getSessionFactory());
    }

    @Test
    public void get_happyPath() throws Exception {
        UUID aliceUuid = database.inTransaction(() -> dao.create(new AchievementProperties("Cook Pasta"))).getId();
        final Achievement actual = dao.get(aliceUuid.toString());
        assertThat(actual.getName()).isEqualTo("Cook Pasta");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_notFound() throws Exception {
        dao.get(UUID.randomUUID().toString());
    }

    @Test
    public void delete_happyPath() throws Exception {
        UUID id = database.inTransaction(() -> dao.create(new AchievementProperties("Something"))).getId();
        database.inTransaction(() -> {
            try {
                dao.delete(id.toString());
            } catch (ObjectNotFoundException e) {
                fail();
            }
        });
        database.inTransaction(() -> {
            try {
                dao.get(id.toString());
                fail();
            } catch (ObjectNotFoundException e) {
            }
        });
    }

    @Test(expected = ObjectNotFoundException.class)
    public void delete_notFound() throws Exception {
        dao.delete(UUID.randomUUID().toString());
    }

    @Test
    public void create_happyPath() throws Exception {
        final Achievement result = database.inTransaction(() -> {
            final AchievementProperties properties = new AchievementProperties("Boil Egg");
            properties.setSteps(Arrays.asList(
                    new AchievementReferenceStep(URI.create("http://localhost/reference/uri")),
                    new AchievementStandardStep("Put egg in water")
            ));
            return dao.create(properties);
        });
        final Achievement actual = database.inTransaction(() -> dao.get(result.getId().toString()));
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getName()).isEqualTo("Boil Egg");

        assertThat(actual.getSteps().get(0)).isInstanceOf(AchievementReferenceStep.class);
        AchievementReferenceStep referenceStep = (AchievementReferenceStep) actual.getSteps().get(0);
        assertThat(referenceStep.getReference()).isEqualTo(URI.create("http://localhost/reference/uri"));

        assertThat(actual.getSteps().get(1)).isInstanceOf(AchievementStandardStep.class);
        AchievementStandardStep standardStep = (AchievementStandardStep) actual.getSteps().get(1);
        assertThat(standardStep.getDescription()).isEqualTo("Put egg in water");
    }

    @Test
    public void update_simpleAchievement_happyPath() throws Exception {
        UUID objectUuid = database.inTransaction(() -> dao.create(new AchievementProperties("Cook Pasta"))).getId();

        database.inTransaction(() -> dao.update(objectUuid.toString(), new AchievementProperties("Cook Spagetti")));

        final Achievement actual = database.inTransaction(() -> dao.get(objectUuid.toString()));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Cook Spagetti");
    }

    @Test
    public void update_complexAchievement_happyPath() throws Exception {
        UUID objectUuid = database.inTransaction(() -> {
            final AchievementProperties initialProperties = new AchievementProperties(
                    "Cook Pasta",
                    Sets.newHashSet("italian", "simple", "tasty"),
                    Lists.newArrayList(
                            new AchievementStandardStep("Step 1"),
                            new AchievementStandardStep("Step 1")
                    ));
            return dao.create(initialProperties);
        }).getId();

        database.inTransaction(() -> {
            final AchievementProperties updatedProperties = new AchievementProperties(
                    "Cook Spagetti",
                    Sets.newHashSet("italian", "quick"),
                    Lists.newArrayList(
                            new AchievementReferenceStep(URI.create("http://localhost")))
                    );
            return dao.update(objectUuid.toString(), updatedProperties);
        });

        final Achievement actual = database.inTransaction(() -> dao.get(objectUuid.toString()));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Cook Spagetti");
        assertThat(actual.getTags()).containsExactlyInAnyOrder("italian", "quick");
        assertThat(actual.getSteps()).hasSize(1);
        assertThat(actual.getSteps().get(0)).isInstanceOf(AchievementReferenceStep.class);
    }
}