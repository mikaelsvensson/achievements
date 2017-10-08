package se.devscout.achievements.server;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AchievementStepProgressAcceptanceTest {
    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    AchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));
    private static String personId;
    private static String ordId;
    private static String stepId;
    private static String achievementId;

    @BeforeClass
    public static void setupAchievement() {
        Client client = RULE.client();

        Response responseAch = client
                .target(String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()))
                .request()
                .post(Entity.json(new AchievementDTO("Solve A Rubik's Cube 2", Collections.emptyList())));

        assertThat(responseAch.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        AchievementDTO achievementDto = responseAch.readEntity(AchievementDTO.class);

        Response responseStep = client
                .target(responseAch.getLocation() + "/steps")
                .request()
                .post(Entity.json(new AchievementStepDTO("Get yourself a Rubik's cube")));

        assertThat(responseStep.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        AchievementStepDTO stepDto = responseStep.readEntity(AchievementStepDTO.class);
        assertThat(stepDto.description).isEqualTo("Get yourself a Rubik's cube");

        Response responseOrg = client
                .target(String.format("http://localhost:%d/api/organizations", RULE.getLocalPort()))
                .request()
                .post(Entity.json(new OrganizationDTO(null, "Name")));

        assertThat(responseOrg.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final Response responsePerson = client
                .target(responseOrg.getLocation() + "/people")
                .request()
                .post(Entity.json(new PersonDTO(null, "Alice")));

        assertThat(responsePerson.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        PersonDTO personDto = responsePerson.readEntity(PersonDTO.class);

        personId = StringUtils.substringAfter(responsePerson.getLocation().toString(), "/people/");
        ordId = StringUtils.substringAfter(responseOrg.getLocation().toString(), "/organizations/");
        stepId = StringUtils.substringAfter(responseStep.getLocation().toString(), "/steps/");
        achievementId = StringUtils.substringAfter(responseAch.getLocation().toString(), "/achievements/");
    }

    @Test
    public void setAndUnsetProgress_happyPath() {
        Client client = RULE.client();

        Response setResponse = client
                .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/person/%s", RULE.getLocalPort(), achievementId, stepId, personId))
                .request()
                .post(Entity.json(new ProgressDTO(true, "Finally completed")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        Response unsetResponse = client
                .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/person/%s", RULE.getLocalPort(), achievementId, stepId, personId))
                .request()
                .delete();

        assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    }

    @Test
    public void setAndUnsetProgress_badAchievementIds() {
        Client client = RULE.client();

        for (String badAchievementId : new String[]{UUID.randomUUID().toString(), "abcd", null, ""}) {
            Response setResponse = client
                    .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/person/%s", RULE.getLocalPort(), badAchievementId, stepId, personId))
                    .request()
                    .post(Entity.json(new ProgressDTO(true, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            Response unsetResponse = client
                    .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/person/%s", RULE.getLocalPort(), badAchievementId, stepId, personId))
                    .request()
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }
    @Test
    public void setAndUnsetProgress_badStepIds() {
        Client client = RULE.client();

        for (String badStepId : new String[]{"-1", "null", null, "0", String.valueOf(Integer.MAX_VALUE)}) {
            Response setResponse = client
                    .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/person/%s", RULE.getLocalPort(), achievementId, badStepId, personId))
                    .request()
                    .post(Entity.json(new ProgressDTO(true, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            Response unsetResponse = client
                    .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/person/%s", RULE.getLocalPort(), achievementId, badStepId, personId))
                    .request()
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }
    @Test
    public void setAndUnsetProgress_badPersonIds() {
        Client client = RULE.client();

        for (String badPersonId : new String[]{"-1", "null", null, "0", String.valueOf(Integer.MAX_VALUE)}) {
            Response setResponse = client
                    .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/person/%s", RULE.getLocalPort(), achievementId, stepId, badPersonId))
                    .request()
                    .post(Entity.json(new ProgressDTO(true, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            Response unsetResponse = client
                    .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/person/%s", RULE.getLocalPort(), achievementId, stepId, badPersonId))
                    .request()
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }

}
