package se.devscout.achievements.server;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Sets;
import io.dropwizard.jersey.jackson.JacksonBinder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.resources.UuidString;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class AchievementStepProgressAcceptanceTest {

    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));
    private static String personId;
    private static String orgId;
    private static String stepId;
    private static String achievementId;

    @BeforeClass
    public static void setupObjectMapper() {
        RULE.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @BeforeClass
    public static void setupAchievement() {
        Client client = createClient();

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
        orgId = StringUtils.substringAfter(responseOrg.getLocation().toString(), "/organizations/");
        stepId = StringUtils.substringAfter(responseStep.getLocation().toString(), "/steps/");
        achievementId = StringUtils.substringAfter(responseAch.getLocation().toString(), "/achievements/");
    }

    @Test
    public void setAndUnsetProgress_happyPath() {
        Client client = createClient();

        final List<StepProgressRequestLogRecordDTO> logRecordsBefore = getProgressHistory(client);

        Response setResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, personId))
                .post(Entity.json(new ProgressDTO(true, "Finally completed")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        Response unsetResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, personId))
                .delete();

        assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        // Verify that only "editors", not "readers", can access the audit log
        Response historyResponseAsReader = TestUtil
                .request(
                        client,
                        URI.create(String.format("http://localhost:%d/api/achievements/%s/progress-history", RULE.getLocalPort(), achievementId)),
                        MockUtil.AUTH_FEATURE_READER)
                .get();
        assertThat(historyResponseAsReader.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        // Verify that two audit records have been added
        final List<StepProgressRequestLogRecordDTO> logRecordsAfter = getProgressHistory(client);

        assertThat(logRecordsAfter).hasSize(logRecordsBefore.size() + 2);

        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).user.name).isEqualTo("Alice Editor");
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).http_method).isEqualTo(HttpMethod.POST);
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).response_code).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).date_time).isNotNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).data.completed).isTrue();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).data.note).isNotEmpty();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).user.name).isEqualTo("Alice Editor");
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).http_method).isEqualTo(HttpMethod.DELETE);
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).response_code).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).date_time).isNotNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).data).isNull();
    }

    private static Client createClient() {
        return RULE.client().register(new JacksonBinder(RULE.getObjectMapper()));
    }

    @Test
    public void setProgressAndTryToDeleteAchievement_expectFailure() {
        Client client = createClient();

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
        Client client = createClient();

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
        Client client = createClient();

        final List<StepProgressRequestLogRecordDTO> logRecordsBefore = getProgressHistory(client);
        String[] linesBefore = TestUtil.getHttpAuditLog(client, RULE.getAdminPort());

        final String[] badValues = {UuidString.toString(UUID.randomUUID()), "abcd", null};
        for (String badAchievementId : badValues) {
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

        // Verify that no progress-specific audit records have been stored (since no successful actions have been performed)
        final List<StepProgressRequestLogRecordDTO> logRecordsAfter = getProgressHistory(client);
        assertThat(logRecordsAfter).hasSameSizeAs(logRecordsBefore);

        // Verify that 6 log records have been recorded for failed actions.
        String[] linesAfter = TestUtil.getHttpAuditLog(client, RULE.getAdminPort());
        final Set<String> logEntriesAddedDuringTest = Sets.difference(
                Sets.newHashSet(linesAfter),
                Sets.newHashSet(linesBefore)
        ).immutableCopy();
        assertThat(logEntriesAddedDuringTest).hasSize(6);
    }

    @Test
    public void setAndUnsetProgress_badStepIds() {
        Client client = createClient();

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
        Client client = createClient();

        for (String badPersonId : new String[]{"-1", "null", null, "0", String.valueOf(Integer.MAX_VALUE)}) {
            Response setResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, badPersonId))
                    .post(Entity.json(new ProgressDTO(true, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            Response unsetResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, badPersonId))
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }

    private List<StepProgressRequestLogRecordDTO> getProgressHistory(Client client) {
        Response historyResponse = TestUtil
                .request(client, String.format("http://localhost:%d/api/achievements/%s/progress-history", RULE.getLocalPort(), achievementId))
                .get();

        assertThat(historyResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        return historyResponse.readEntity(new GenericType<List<StepProgressRequestLogRecordDTO>>() {
        });
    }

}
