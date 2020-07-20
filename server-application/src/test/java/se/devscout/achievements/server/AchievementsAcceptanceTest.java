package se.devscout.achievements.server;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.api.AchievementStepDTO;

import javax.ws.rs.client.Entity;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class AchievementsAcceptanceTest {
    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));

    @Test
    public void createAchievement_happyPath() {
        var client = RULE.client();

        var response = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()), MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementDTO("Solve A Rubik's Cube 1", Collections.emptyList())));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var responseDto = response.readEntity(AchievementDTO.class);

        final var expectedLocation = URI.create(String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), responseDto.id));
        final var actualLocation = response.getLocation();
        assertThat(actualLocation).isEqualTo(expectedLocation);
    }

    @Test
    public void createAchievementAndSteps_happyPath() {
        var client = RULE.client();

        var response = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()), MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementDTO("Solve A Rubik's Cube 2", Collections.emptyList())));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        var responseStep = TestUtil.request(client, response.getLocation() + "/steps", MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementStepDTO("Get yourself a Rubik's cube")));

        assertThat(responseStep.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var stepDto = responseStep.readEntity(AchievementStepDTO.class);
        assertThat(stepDto.description).isEqualTo("Get yourself a Rubik's cube");
    }

    @Test
    public void createAndUpdateAchievementWithSteps_happyPath() {
        var client = RULE.client();

        var createResp = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()), MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementDTO("Solve A Rubik's Cube 3", Arrays.asList(
                        new AchievementStepDTO("Get yourself a Rubik's cube"),
                        new AchievementStepDTO("Solve it"),
                        new AchievementStepDTO("Be smug about it")
                ))));

        assertThat(createResp.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final var createdObj = createResp.readEntity(AchievementDTO.class);

        assertThat(createdObj.id).isNotNull();
        assertThat(createdObj.name).isEqualTo("Solve A Rubik's Cube 3");
        assertThat(createdObj.steps).hasSize(3);
        assertThat(createdObj.steps.get(0).description).isEqualTo("Get yourself a Rubik's cube");
        assertThat(createdObj.steps.get(1).description).isEqualTo("Solve it");
        assertThat(createdObj.steps.get(2).description).isEqualTo("Be smug about it");

        final var readResp = TestUtil.request(client, createResp.getLocation()).get();
        final var readObj = readResp.readEntity(AchievementDTO.class);

        assertThat(readObj.id).isNotNull();
        assertThat(readObj.name).isEqualTo("Solve A Rubik's Cube 3");
        assertThat(readObj.steps).hasSize(3);
        assertThat(readObj.steps.get(0).id).isGreaterThan(0);
        assertThat(readObj.steps.get(0).description).isEqualTo("Get yourself a Rubik's cube");
        assertThat(readObj.steps.get(1).id).isGreaterThan(0);
        assertThat(readObj.steps.get(1).description).isEqualTo("Solve it");
        assertThat(readObj.steps.get(2).id).isGreaterThan(0);
        assertThat(readObj.steps.get(2).description).isEqualTo("Be smug about it");

        final var highestIdBefore = readObj.steps.stream().map(step -> step.id).max(Integer::compareTo).orElseThrow();
        final var solveItStepId = readObj.steps.get(1).id;

        // Change description of existing step (should trigger UPDATE)
        readObj.steps.get(1).description = "Solve it, if you can.";
        // Remove the existing "smug" step (should trigger DELETE)
        readObj.steps.remove(2);
        // Add new step to the middle of the list
        readObj.steps.add(1, new AchievementStepDTO("Read instructions online"));

        var updateResp = TestUtil.request(client, createResp.getLocation(), MockUtil.AUTH_FEATURE_ADMIN)
                .put(Entity.json(readObj));

        assertThat(updateResp.getStatus()).isEqualTo(HttpStatus.OK_200);
        final var updatedObj = updateResp.readEntity(AchievementDTO.class);

        assertThat(updatedObj.id).isNotNull();
        assertThat(updatedObj.name).isEqualTo("Solve A Rubik's Cube 3");
        assertThat(updatedObj.steps).hasSize(3);
        // First step is unchanged
        assertThat(updatedObj.steps.get(0).id).isEqualTo(readObj.steps.get(0).id);
        assertThat(updatedObj.steps.get(0).description).isEqualTo("Get yourself a Rubik's cube");
        // Second step is new (with id greater than of any previous step)
        assertThat(updatedObj.steps.get(1).id).isGreaterThan(highestIdBefore);
        assertThat(updatedObj.steps.get(1).description).isEqualTo("Read instructions online");
        // Third step is updated
        assertThat(updatedObj.steps.get(2).id).isEqualTo(solveItStepId);
        assertThat(updatedObj.steps.get(2).description).isEqualTo("Solve it, if you can.");
        // ...and the be-slug-about-it step has been deleted.
    }

    @Test
    public void createAchievementWithReference_happyPath() {
        var client = RULE.client();

        var responseA = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()), MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementDTO("Learn to ride bicycle", Collections.emptyList())));
        assertThat(responseA.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var achievementBicycleId = StringUtils.substringAfter(responseA.getLocation().toString(), "/achievements/");

        var responseB = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()), MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementDTO("Learn to ride a motorcycle", Collections.emptyList())));
        assertThat(responseB.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        var responseStep1 = TestUtil.request(client, responseB.getLocation() + "/steps", MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(AchievementStepDTO.withPrerequisite(achievementBicycleId)));
        assertThat(responseStep1.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        var responseStep2 = TestUtil.request(client, responseB.getLocation() + "/steps", MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementStepDTO("Learn traffic rules for highways")));
        assertThat(responseStep2.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        var responseGetB = TestUtil.request(client, responseB.getLocation())
                .get();
        assertThat(responseGetB.getStatus()).isEqualTo(HttpStatus.OK_200);
        var getResponse = responseGetB.readEntity(AchievementDTO.class);
        assertThat(getResponse.name).isEqualTo("Learn to ride a motorcycle");
        assertThat(getResponse.steps).hasSize(2);
        assertThat(getResponse.steps.get(0).description).isNull();
        assertThat(getResponse.steps.get(0).prerequisite_achievement).isEqualTo(achievementBicycleId);
        assertThat(getResponse.steps.get(1).description).isEqualTo("Learn traffic rules for highways");
        assertThat(getResponse.steps.get(1).prerequisite_achievement).isNull();
    }

}
