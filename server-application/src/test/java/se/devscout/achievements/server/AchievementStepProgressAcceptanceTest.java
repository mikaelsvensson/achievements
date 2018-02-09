package se.devscout.achievements.server;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.resources.UuidString;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AchievementStepProgressAcceptanceTest {

    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));
    private static String personId;
    private static String ordId;
    private static String stepId;
    private static String achievementId;

    @BeforeClass
    public static void setupAchievement() {
        Client client = RULE.client();

        Response responseAch = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()))
                .post(Entity.json(new AchievementDTO("Solve A Rubik's Cube 2", Collections.emptyList())));

        assertThat(responseAch.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        AchievementDTO achievementDto = responseAch.readEntity(AchievementDTO.class);

        Response responseStep = TestUtil.request(client, responseAch.getLocation() + "/steps")
                .post(Entity.json(new AchievementStepDTO("Get yourself a Rubik's cube")));

        assertThat(responseStep.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        AchievementStepDTO stepDto = responseStep.readEntity(AchievementStepDTO.class);
        assertThat(stepDto.description).isEqualTo("Get yourself a Rubik's cube");

        Response responseOrg = TestUtil.request(client, String.format("http://localhost:%d/api/organizations", RULE.getLocalPort()))
                .post(Entity.json(new OrganizationDTO(null, "Name")));

        assertThat(responseOrg.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final Response responsePerson = TestUtil.request(client, responseOrg.getLocation() + "/people")
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

        Response setResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, personId))
                .post(Entity.json(new ProgressDTO(true, "Finally completed")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        Response unsetResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, personId))
                .delete();

        assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    }

    @Test
    public void setProgressAndTryToDeleteAchievement_expectFailure() {
        Client client = RULE.client();

        Response setResponse = TestUtil.request(client,
                String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, personId)
        ).post(Entity.json(new ProgressDTO(true, "Finally completed")));
        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        Response deleteAchievementResponse = TestUtil.request(client,
                String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), achievementId)
        ).delete();
        assertThat(deleteAchievementResponse.getStatus()).isEqualTo(HttpStatus.CONFLICT_409);

        Response getAchievementResponse = TestUtil.request(client,
                String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), achievementId)
        ).get();
        assertThat(getAchievementResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        Response deleteStepResponse = TestUtil.request(client,
                String.format("http://localhost:%d/api/achievements/%s/steps/%s", RULE.getLocalPort(), achievementId, stepId)
        ).delete();
        assertThat(deleteStepResponse.getStatus()).isEqualTo(HttpStatus.CONFLICT_409);

        Response getStepResponse = TestUtil.request(client,
                String.format("http://localhost:%d/api/achievements/%s/steps/%s", RULE.getLocalPort(), achievementId, stepId)
        ).get();
        assertThat(getStepResponse.getStatus()).isEqualTo(HttpStatus.OK_200);
    }

    @Test
    public void setMultipleProgress_happyPath() {
        Client client = RULE.client();

        Response setResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, personId))
                .post(Entity.json(new ProgressDTO(true, "Finally completed")));
        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        Response getResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/progress", RULE.getLocalPort(), achievementId))
                .get();
        assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        final Map<String, ProgressDTO> dto = getResponse.readEntity(new GenericType<Map<String, ProgressDTO>>() {
        });
        assertThat(dto).containsKey(stepId + "_" + personId);
        assertThat(dto.get(stepId + "_" + personId).note).isEqualTo("Finally completed");
    }

    @Test
    public void setAndUnsetProgress_badAchievementIds() {
        Client client = RULE.client();

        for (String badAchievementId : new String[]{UuidString.toString(UUID.randomUUID()), "abcd", null, ""}) {
            Response setResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), badAchievementId, stepId, personId))
                    .post(Entity.json(new ProgressDTO(true, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            Response unsetResponse = client
                    .target(String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), badAchievementId, stepId, personId))
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }

    @Test
    public void setAndUnsetProgress_badStepIds() {
        Client client = RULE.client();

        for (String badStepId : new String[]{"-1", "null", null, "0", String.valueOf(Integer.MAX_VALUE)}) {
            Response setResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, badStepId, personId))
                    .post(Entity.json(new ProgressDTO(true, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            Response unsetResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, badStepId, personId))
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }

    @Test
    public void setAndUnsetProgress_badPersonIds() {
        Client client = RULE.client();

        for (String badPersonId : new String[]{"-1", "null", null, "0", String.valueOf(Integer.MAX_VALUE)}) {
            Response setResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, badPersonId))
                    .post(Entity.json(new ProgressDTO(true, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            Response unsetResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, badPersonId))
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }

}
