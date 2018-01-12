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
        Client client = RULE.client();

        final Response response = TestUtil.request(client, String.format("http://localhost:%d/tasks/bootstrap-data", RULE.getAdminPort()))
                .post(null);
        final String bootstrapResponse = response.readEntity(String.class);
        final Matcher matcher = Pattern.compile("Created organization.* \\(id (.+)\\)").matcher(bootstrapResponse);
        matcher.find();
        organizationId = matcher.group(1);
    }

    @Test
    public void createUpdateGet_happyPath() {
        Client client = RULE.client();

        final GroupDTO dto = new GroupDTO(null, "The Developers");

        Response createResponse = TestUtil.request(client, String.format("http://localhost:%d/api/organizations/%s/groups", RULE.getLocalPort(), organizationId))
                .post(Entity.json(dto));

        assertThat(createResponse.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final URI location = createResponse.getLocation();

        dto.name = "Devs";

        Response updateResponse = TestUtil.request(client, location)
                .put(Entity.json(dto));

        assertThat(updateResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        final GroupDTO response2Dto = updateResponse.readEntity(GroupDTO.class);

        assertThat(response2Dto.name).isEqualTo("Devs");
        assertThat(response2Dto.people).isNullOrEmpty();

        Response getResponse = TestUtil.request(client, location)
                .get();

        assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.OK_200);

        final GroupDTO response3Dto = getResponse.readEntity(GroupDTO.class);

        assertThat(response3Dto.name).isEqualTo("Devs");
    }

    @Test
    public void createDelete_happyPath() {
        Client client = RULE.client();

        final GroupDTO dto = new GroupDTO(null, "The Legal Department");

        Response createResponse = TestUtil.request(client, String.format("http://localhost:%d/api/organizations/%s/groups", RULE.getLocalPort(), organizationId))
                .post(Entity.json(dto));

        assertThat(createResponse.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final URI location = createResponse.getLocation();

        Response deleteResponse = TestUtil.request(client, location)
                .delete();

        assertThat(deleteResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        Response getResponse = TestUtil.request(client, location)
                .get();

        assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void create_missingName_expect400() {
        Client client = RULE.client();

        final GroupDTO dto = new GroupDTO(null, "");

        Response createResponse = TestUtil.request(client, String.format("http://localhost:%d/api/organizations/%s/groups", RULE.getLocalPort(), organizationId))
                .post(Entity.json(dto));

        assertThat(createResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        assertThat(createResponse.getHeaderString("Content-Type")).contains("application/json");

        final ObjectNode actualResponseEntity = createResponse.readEntity(ObjectNode.class);
        assertThat(actualResponseEntity.has("message")).isTrue();
        assertThat(actualResponseEntity.get("status").asInt()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    }

}
