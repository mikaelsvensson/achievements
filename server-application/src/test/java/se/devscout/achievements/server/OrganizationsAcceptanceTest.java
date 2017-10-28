package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.resources.UuidString;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationsAcceptanceTest {
    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));

    @Test
    public void create_happyPath() {
        Client client = RULE.client();

        Response response = client
                .target(String.format("http://localhost:%d/api/organizations", RULE.getLocalPort()))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new OrganizationDTO(null, "Name")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final OrganizationDTO responseDto = response.readEntity(OrganizationDTO.class);

        final URI expectedLocation = URI.create(String.format("http://localhost:%d/api/organizations/%s", RULE.getLocalPort(), responseDto.id));
        final URI actualLocation = response.getLocation();
        assertThat(actualLocation).isEqualTo(expectedLocation);
    }

    @Test
    public void update_happyPath() {
        Client client = RULE.client();

        final OrganizationDTO dto = new OrganizationDTO(null, "Name To Update");
        Response responseCreate = client
                .target(String.format("http://localhost:%d/api/organizations", RULE.getLocalPort()))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .post(Entity.json(dto));

        assertThat(responseCreate.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        dto.name = "New Name";

        Response responseUpdate = client
                .target(responseCreate.getLocation())
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .put(Entity.json(dto));

        assertThat(responseUpdate.getStatus()).isEqualTo(HttpStatus.OK_200);

        final OrganizationDTO responseDto = responseUpdate.readEntity(OrganizationDTO.class);

        assertThat(responseDto.name).isEqualTo(dto.name);

        Response responseGet = client
                .target(responseCreate.getLocation())
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get();

        final OrganizationDTO responseGetDto = responseGet.readEntity(OrganizationDTO.class);

        assertThat(responseGetDto.name).isEqualTo(dto.name);

    }

}
