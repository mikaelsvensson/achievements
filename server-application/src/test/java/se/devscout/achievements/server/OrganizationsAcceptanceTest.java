package se.devscout.achievements.server;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.OrganizationDTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationsAcceptanceTest {
    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));

    @Test
    public void create_happyPath() {
        var client = RULE.client();

        var response = TestUtil.request(client, String.format("http://localhost:%d/api/organizations", RULE.getLocalPort()))
                .post(Entity.json(new OrganizationDTO(null, "Name")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final var responseDto = response.readEntity(OrganizationDTO.class);

        final var expectedLocation = URI.create(String.format("http://localhost:%d/api/organizations/%s", RULE.getLocalPort(), responseDto.id));
        final var actualLocation = response.getLocation();
        assertThat(actualLocation).isEqualTo(expectedLocation);
    }

    @Test
    public void update_happyPath() {
        var client = RULE.client();

        final var dto = new OrganizationDTO(null, "Name To Update");
        var responseCreate = TestUtil.request(client, String.format("http://localhost:%d/api/organizations", RULE.getLocalPort()))
                .post(Entity.json(dto));

        assertThat(responseCreate.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        dto.name = "New Name";

        var responseUpdate = TestUtil.request(client, responseCreate.getLocation())
                .put(Entity.json(dto));

        assertThat(responseUpdate.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var responseDto = responseUpdate.readEntity(OrganizationDTO.class);

        assertThat(responseDto.name).isEqualTo(dto.name);

        var responseGet = TestUtil.request(client, responseCreate.getLocation())
                .get();

        final var responseGetDto = responseGet.readEntity(OrganizationDTO.class);

        assertThat(responseGetDto.name).isEqualTo(dto.name);

    }

}
