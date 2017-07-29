package se.devscout.achievements.server;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.OrganizationDTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationsAcceptanceTest {
    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(AchievementsApplication.class, ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));

    @Test
    public void create_happyPath() {
        Client client = RULE.client();

        Response response = client
                .target(String.format("http://localhost:%d/organizations", RULE.getLocalPort()))
                .request()
                .post(Entity.json(new OrganizationDTO(null, "Name")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final OrganizationDTO responseDto = response.readEntity(OrganizationDTO.class);
        assertThat(response.getLocation()).isEqualTo(URI.create(String.format("http://localhost:%d/organizations/%s", RULE.getLocalPort(), responseDto.id)));
    }

}
