package se.devscout.achievements.server;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.api.AchievementStepDTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
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

        var response = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()))
                .post(Entity.json(new AchievementDTO("Solve A Rubik's Cube 1", Collections.emptyList())));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var responseDto = response.readEntity(AchievementDTO.class);

        final var expectedLocation = URI.create(String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), responseDto.id));
        final var actualLocation = response.getLocation();
        assertThat(actualLocation).isEqualTo(expectedLocation);
    }

    @Test
    public void createAchievementWithSteps_happyPath() {
        var client = RULE.client();

        var response = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()))
                .post(Entity.json(new AchievementDTO("Solve A Rubik's Cube 2", Collections.emptyList())));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        var responseStep = TestUtil.request(client, response.getLocation() + "/steps")
                .post(Entity.json(new AchievementStepDTO("Get yourself a Rubik's cube")));

        assertThat(responseStep.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var stepDto = responseStep.readEntity(AchievementStepDTO.class);
        assertThat(stepDto.description).isEqualTo("Get yourself a Rubik's cube");
    }

    @Test
    public void createAchievementWithReference_happyPath() {
        var client = RULE.client();

        var responseA = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()))
                .post(Entity.json(new AchievementDTO("Learn to ride bicycle", Collections.emptyList())));
        assertThat(responseA.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var achievementBicycleId = StringUtils.substringAfter(responseA.getLocation().toString(), "/achievements/");

        var responseB = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()))
                .post(Entity.json(new AchievementDTO("Learn to ride a motorcycle", Collections.emptyList())));
        assertThat(responseB.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        var responseStep1 = TestUtil.request(client, responseB.getLocation() + "/steps")
                .post(Entity.json(AchievementStepDTO.withPrerequisite(achievementBicycleId)));
        assertThat(responseStep1.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        var responseStep2 = TestUtil.request(client, responseB.getLocation() + "/steps")
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
