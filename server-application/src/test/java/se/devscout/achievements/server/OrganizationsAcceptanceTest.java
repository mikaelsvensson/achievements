package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.OrganizationDTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationsAcceptanceTest {
    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    App.class,
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

}
