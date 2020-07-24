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
import se.devscout.achievements.server.data.model.AchievementStepProgressProperties;
import se.devscout.achievements.server.resources.InProgressCheck;
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
    private static String stepId;
    private static String achievementId;
    private static String step2Id;
    private static String achievement2Id;

    @BeforeClass
    public static void setupObjectMapper() {
        RULE.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @BeforeClass
    public static void setupAchievement() {
        var client = createClient();

        final URI achLocation = createAchievement(client, "Solve A Rubik's Cube 2");
        achievementId = StringUtils.substringAfter(achLocation.toString(), "/achievements/");

        final URI stepLocation = createStep(client, achLocation);
        stepId = StringUtils.substringAfter(stepLocation.toString(), "/steps/");

        final URI achLocation2 = createAchievement(client, "Solve A Rubik's Cube 3");
        achievement2Id = StringUtils.substringAfter(achLocation2.toString(), "/achievements/");

        final URI stepLocation2 = createStep(client, achLocation2);
        step2Id = StringUtils.substringAfter(stepLocation2.toString(), "/steps/");

        final URI personLocation = createPerson(client);
        personId = StringUtils.substringAfter(personLocation.toString(), "/people/");
    }

    private static URI createPerson(Client client) {
        var responseOrg = TestUtil.request(client, String.format("http://localhost:%d/api/organizations", RULE.getLocalPort()), MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new OrganizationDTO(null, "Name")));

        assertThat(responseOrg.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var responsePerson = TestUtil.request(client, responseOrg.getLocation() + "/people", MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new PersonDTO(null, "Alice")));

        assertThat(responsePerson.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        return responsePerson.getLocation();
    }

    private static URI createStep(Client client, URI achLocation) {
        var responseStep = TestUtil.request(client, achLocation + "/steps", MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementStepDTO("Get yourself a Rubik's cube")));

        assertThat(responseStep.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        var stepDto = responseStep.readEntity(AchievementStepDTO.class);
        assertThat(stepDto.description).isEqualTo("Get yourself a Rubik's cube");

        return responseStep.getLocation();
    }

    private static URI createAchievement(Client client, String name) {
        var responseAch = TestUtil.request(client, String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()), MockUtil.AUTH_FEATURE_ADMIN)
                .post(Entity.json(new AchievementDTO(name, Collections.emptyList())));

        assertThat(responseAch.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        return responseAch.getLocation();
    }

    @Test
    public void setAndUnsetProgress_happyPath() {
        var client = createClient();

        final var logRecordsBefore = getProgressHistory(client);

        {
            var setResponse1 = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                    .post(Entity.json(new ProgressDTO(null, 25, "Just started")));

            assertThat(setResponse1.getStatus()).isEqualTo(HttpStatus.OK_200);
            assertProgress(client, 25, false, "Just started");
        }

        {
            var setResponse2 = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                    .post(Entity.json(new ProgressDTO(true, AchievementStepProgressProperties.PROGRESS_COMPLETED, "Finally completed")));

            assertThat(setResponse2.getStatus()).isEqualTo(HttpStatus.OK_200);
            assertProgress(client, 100, true, "Finally completed");
        }

        {
            var setResponse3 = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                    .post(Entity.json(new ProgressDTO(null, 75, "Almost there")));

            assertThat(setResponse3.getStatus()).isEqualTo(HttpStatus.OK_200);
            assertProgress(client, 75, false, "Almost there");
        }

        {
            var unsetResponse = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
            assertProgressNotSet(client);
        }

        {
            var setAgainResponse = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                    .post(Entity.json(new ProgressDTO(null, 50, "I changed my mind")));

            assertThat(setAgainResponse.getStatus()).isEqualTo(HttpStatus.OK_200);
            assertProgress(client, 50, false, "I changed my mind");
        }

        // Verify that only "editors", not "readers", can access the audit log
        var historyResponseAsReader = TestUtil
                .request(
                        client,
                        URI.create(String.format("http://localhost:%d/api/achievements/%s/progress-history", RULE.getLocalPort(), achievementId)),
                        MockUtil.AUTH_FEATURE_READER)
                .get();
        assertThat(historyResponseAsReader.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        // Verify that two audit records have been added
        final var logRecordsAfter = getProgressHistory(client);

        assertThat(logRecordsAfter).hasSize(logRecordsBefore.size() + 5);

        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).user.name).isEqualTo("Alice Editor");
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).http_method).isEqualTo(HttpMethod.POST);
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).response_code).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).date_time).isNotNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).data.completed).isNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 0).data.note).isNotEmpty();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).user.name).isEqualTo("Alice Editor");
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).http_method).isEqualTo(HttpMethod.POST);
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).response_code).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).date_time).isNotNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).data.completed).isTrue();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 1).data.note).isNotEmpty();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 2).user.name).isEqualTo("Alice Editor");
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 2).http_method).isEqualTo(HttpMethod.POST);
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 2).response_code).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 2).date_time).isNotNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 2).data.completed).isNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 2).data.note).isNotEmpty();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 3).user.name).isEqualTo("Alice Editor");
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 3).http_method).isEqualTo(HttpMethod.DELETE);
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 3).response_code).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 3).date_time).isNotNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 3).data).isNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 4).user.name).isEqualTo("Alice Editor");
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 4).http_method).isEqualTo(HttpMethod.POST);
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 4).response_code).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 4).date_time).isNotNull();
        assertThat(logRecordsAfter.get(logRecordsBefore.size() + 4).data).isNotNull();
    }

    @Test
    public void setAndUnsetProgress_partial000HappyPath() {
        var client = createClient();

        var setResponse = TestUtil
                .request(client, progressEndpoint(personId, achievementId, stepId))
                .post(Entity.json(new ProgressDTO(null, 0, "Not even started")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        assertProgress(client, 0, false, "Not even started");
    }

    @Test
    public void setAndUnsetProgress_partial050HappyPath() {
        var client = createClient();

        var setResponse = TestUtil
                .request(client, progressEndpoint(personId, achievementId, stepId))
                .post(Entity.json(new ProgressDTO(null, 50, "Half-way there")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        assertProgress(client, 50, false, "Half-way there");
    }

    @Test
    public void setAndUnsetProgress_partial100HappyPath() {
        var client = createClient();

        var setResponse = TestUtil
                .request(client, progressEndpoint(personId, achievementId, stepId))
                .post(Entity.json(new ProgressDTO(null, 100, "Finally there")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        assertProgress(client, 100, true, "Finally there");
    }

    private static Client createClient() {
        return RULE.client().register(new JacksonBinder(RULE.getObjectMapper()));
    }

    @Test
    public void setProgressAndTryToDeleteAchievement_expectFailure() {
        var client = createClient();

        // ARRANGE
        var setResponse = TestUtil.request(
                client,
                progressEndpoint(personId, achievementId, stepId)
        ).post(Entity.json(new ProgressDTO(true, AchievementStepProgressProperties.PROGRESS_COMPLETED, "Finally completed")));
        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        // ACT
        var deleteAchievementResponse = TestUtil.request(
                client,
                String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), achievementId),
                MockUtil.AUTH_FEATURE_ADMIN
        ).delete();
        assertThat(deleteAchievementResponse.getStatus()).isEqualTo(HttpStatus.CONFLICT_409);

        // ASSESS
        var getAchievementResponse = TestUtil.request(
                client,
                String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), achievementId)
        ).get();
        assertThat(getAchievementResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        // ACT
        var deleteStepResponse = TestUtil.request(
                client,
                String.format("http://localhost:%d/api/achievements/%s/steps/%s", RULE.getLocalPort(), achievementId, stepId),
                MockUtil.AUTH_FEATURE_ADMIN
        ).delete();
        assertThat(deleteStepResponse.getStatus()).isEqualTo(HttpStatus.CONFLICT_409);

        // ASSESS
        var getStepResponse = TestUtil.request(
                client,
                String.format("http://localhost:%d/api/achievements/%s/steps/%s", RULE.getLocalPort(), achievementId, stepId)
        ).get();
        assertThat(getStepResponse.getStatus()).isEqualTo(HttpStatus.OK_200);
    }

    @Test
    public void setProgressAndDeleteAchievement_expectFailure() {
        var client = createClient();

        // ARRANGE
        var setResponse = TestUtil.request(
                client,
                progressEndpoint(personId, achievement2Id, step2Id)
        ).post(Entity.json(new ProgressDTO(true, AchievementStepProgressProperties.PROGRESS_COMPLETED, "Finally completed")));
        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        // ACT
        var deleteAchievementResponse2 = TestUtil.request(
                client,
                String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), achievement2Id),
                MockUtil.AUTH_FEATURE_ADMIN
        ).header(AchievementsApplication.HEADER_IN_PROGRESS_CHECK, InProgressCheck.SKIP.name()).delete();
        assertThat(deleteAchievementResponse2.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        // ASSESS
        var getAchievementResponse2 = TestUtil.request(
                client,
                String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), achievement2Id)
        ).get();
        assertThat(getAchievementResponse2.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        // ASSESS
        var getStepResponse2 = TestUtil.request(
                client,
                String.format("http://localhost:%d/api/achievements/%s/steps/%s", RULE.getLocalPort(), achievement2Id, step2Id)
        ).get();
        assertThat(getStepResponse2.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void setMultipleProgress_happyPath() {
        var client = createClient();

        var setResponse = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                .post(Entity.json(new ProgressDTO(true, AchievementStepProgressProperties.PROGRESS_COMPLETED, "Finally completed")));
        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        var getResponse = TestUtil.request(client, String.format("http://localhost:%d/api/achievements/%s/progress", RULE.getLocalPort(), achievementId))
                .get();
        assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        final Map<String, ProgressDTO> dto = getResponse.readEntity(new GenericType<>() {
        });
        assertThat(dto).containsKey(stepId + "_" + personId);
        assertThat(dto.get(stepId + "_" + personId).note).isEqualTo("Finally completed");
    }

    @Test
    public void setAndUnsetProgress_badAchievementIds() {
        var client = createClient();

        final var logRecordsBefore = getProgressHistory(client);
        var linesBefore = TestUtil.getHttpAuditLog(client, RULE.getAdminPort());

        final String[] badValues = {UuidString.toString(UUID.randomUUID()), "abcd", null};
        for (var badAchievementId : badValues) {
            var setResponse = TestUtil.request(client, progressEndpoint(personId, badAchievementId, stepId))
                    .post(Entity.json(new ProgressDTO(true, AchievementStepProgressProperties.PROGRESS_COMPLETED, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            var unsetResponse = client
                    .target(progressEndpoint(personId, badAchievementId, stepId))
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

        // Verify that no progress-specific audit records have been stored (since no successful actions have been performed)
        final var logRecordsAfter = getProgressHistory(client);
        assertThat(logRecordsAfter).hasSameSizeAs(logRecordsBefore);

        // Verify that 6 log records have been recorded for failed actions.
        var linesAfter = TestUtil.getHttpAuditLog(client, RULE.getAdminPort());
        final Set<String> logEntriesAddedDuringTest = Sets.difference(
                Sets.newHashSet(linesAfter),
                Sets.newHashSet(linesBefore)
        ).immutableCopy();
        assertThat(logEntriesAddedDuringTest).hasSize(6);
    }

    @Test
    public void setAndUnsetProgress_inconsistentProgressAndCompleted() {
        var client = createClient();

        var setResponse = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                .post(Entity.json(new ProgressDTO(true, 99, "Finally completed, sort of.")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    }

    @Test
    public void setAndUnsetProgress_neitherProgressNorCompleted() {
        var client = createClient();

        var setResponse = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                .post(Entity.json(new ProgressDTO(null, null, "Neither one nor the other")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    }

    @Test
    public void setAndUnsetProgress_invalidProgressValue() {
        var client = createClient();

        var setResponse = TestUtil.request(client, progressEndpoint(personId, achievementId, stepId))
                .post(Entity.json(new ProgressDTO(null, 10000, "Too much progress")));

        assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    }

    @Test
    public void setAndUnsetProgress_badStepIds() {
        var client = createClient();

        for (var badStepId : new String[]{"-1", "null", null, "0", String.valueOf(Integer.MAX_VALUE)}) {
            var setResponse = TestUtil.request(client, progressEndpoint(personId, achievementId, badStepId))
                    .post(Entity.json(new ProgressDTO(true, AchievementStepProgressProperties.PROGRESS_COMPLETED, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            var unsetResponse = TestUtil.request(client, progressEndpoint(personId, achievementId, badStepId))
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }

    @Test
    public void setAndUnsetProgress_badPersonIds() {
        var client = createClient();

        for (var badPersonId : new String[]{"-1", "null", null, "0", String.valueOf(Integer.MAX_VALUE)}) {
            var setResponse = TestUtil.request(client, progressEndpoint(badPersonId, achievementId, stepId))
                    .post(Entity.json(new ProgressDTO(true, AchievementStepProgressProperties.PROGRESS_COMPLETED, "Finally completed")));

            assertThat(setResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

            var unsetResponse = TestUtil.request(client, progressEndpoint(badPersonId, achievementId, stepId))
                    .delete();

            assertThat(unsetResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

    }

    private String progressEndpoint(String personId, String achievementId, String stepId) {
        return String.format("http://localhost:%d/api/achievements/%s/steps/%s/progress/%s", RULE.getLocalPort(), achievementId, stepId, personId);
    }

    private void assertProgressNotSet(Client client) {
        var getResponse = TestUtil
                .request(client, progressEndpoint(personId, achievementId, stepId))
                .get();

        assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    private void assertProgress(Client client, int expectedValue, boolean expectedCompleted, String expectedNote) {
        var getResponse = TestUtil
                .request(client, progressEndpoint(personId, achievementId, stepId))
                .get();

        assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var dto = getResponse.readEntity(ProgressDTO.class);
        assertThat(dto.value).isEqualTo(expectedValue);
        assertThat(dto.completed).isEqualTo(expectedCompleted);
        assertThat(dto.note).isEqualTo(expectedNote);
    }

    private List<StepProgressRequestLogRecordDTO> getProgressHistory(Client client) {
        var historyResponse = TestUtil
                .request(client, String.format("http://localhost:%d/api/achievements/%s/progress-history", RULE.getLocalPort(), achievementId))
                .get();

        assertThat(historyResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        return historyResponse.readEntity(new GenericType<>() {
        });
    }

}
