package se.devscout.achievements.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.GroupDTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupsAcceptanceTest {
    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));
    private static String organizationId;


    @BeforeClass
    public static void setUp() throws Exception {
        var client = RULE.client();

        final var response = TestUtil.request(client, String.format("http://localhost:%d/tasks/bootstrap-data", RULE.getAdminPort()))
                .post(null);
        final var bootstrapResponse = response.readEntity(String.class);
        final var matcher = Pattern.compile("Created organization.* \\(id (.+)\\)").matcher(bootstrapResponse);
        matcher.find();
        organizationId = matcher.group(1);
    }

    @Test
    public void createUpdateGet_happyPath() {
        var client = RULE.client();

        final var dto = new GroupDTO(null, "The Developers");

        var createResponse = TestUtil.request(client, String.format("http://localhost:%d/api/organizations/%s/groups", RULE.getLocalPort(), organizationId))
                .post(Entity.json(dto));

        assertThat(createResponse.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var location = createResponse.getLocation();

        dto.name = "Devs";

        var updateResponse = TestUtil.request(client, location)
                .put(Entity.json(dto));

        assertThat(updateResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var response2Dto = updateResponse.readEntity(GroupDTO.class);

        assertThat(response2Dto.name).isEqualTo("Devs");
        assertThat(response2Dto.people).isNullOrEmpty();

        var getResponse = TestUtil.request(client, location)
                .get();

        assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var response3Dto = getResponse.readEntity(GroupDTO.class);

        assertThat(response3Dto.name).isEqualTo("Devs");
    }

    @Test
    public void createDelete_happyPath() {
        var client = RULE.client();

        final var dto = new GroupDTO(null, "The Legal Department");

        var createResponse = TestUtil.request(client, String.format("http://localhost:%d/api/organizations/%s/groups", RULE.getLocalPort(), organizationId))
                .post(Entity.json(dto));

        assertThat(createResponse.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var location = createResponse.getLocation();

        var deleteResponse = TestUtil.request(client, location)
                .delete();

        assertThat(deleteResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        var getResponse = TestUtil.request(client, location)
                .get();

        assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void create_missingName_expect400() {
        var client = RULE.client();

        final var dto = new GroupDTO(null, "");

        var createResponse = TestUtil.request(client, String.format("http://localhost:%d/api/organizations/%s/groups", RULE.getLocalPort(), organizationId))
                .post(Entity.json(dto));

        assertThat(createResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        assertThat(createResponse.getHeaderString("Content-Type")).contains("application/json");

        final var actualResponseEntity = createResponse.readEntity(ObjectNode.class);
        assertThat(actualResponseEntity.has("message")).isTrue();
        assertThat(actualResponseEntity.get("status").asInt()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    }

}
