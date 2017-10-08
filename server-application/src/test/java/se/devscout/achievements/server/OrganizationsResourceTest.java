package se.devscout.achievements.server;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.TooManyOrganizationsException;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.OrganizationProperties;
import se.devscout.achievements.server.resources.OrganizationsResource;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class OrganizationsResourceTest {

    private final OrganizationsDao dao = mock(OrganizationsDao.class);

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new OrganizationsResource(dao))
            .build();

    @Test
    public void get_happyPath() throws Exception {
        final UUID uuid = UUID.randomUUID();
        when(dao.read(eq(uuid))).thenReturn(new Organization(uuid, "Alice's Organization"));
        final OrganizationDTO dto = resources
                .target("/organizations/" + uuid.toString())
                .request()
                .get(OrganizationDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.name).isEqualTo("Alice's Organization");
    }

    @Test
    public void create_happyPath() throws Exception {
        when(dao.create(any(OrganizationProperties.class))).thenAnswer(invocation -> new Organization(UUID.randomUUID(), ((OrganizationProperties)invocation.getArgument(0)).getName()));
        final Response response = resources
                .target("/organizations")
                .request()
                .post(Entity.json(new OrganizationDTO(null, "Bob's Club")));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final OrganizationDTO dto = response.readEntity(OrganizationDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.name).isEqualTo("Bob's Club");
    }

    @Test
    public void create_tooManyOrganizations() throws Exception {
        when(dao.create(any(OrganizationProperties.class))).thenThrow(new TooManyOrganizationsException());
        final Response response = resources
                .target("/organizations")
                .request()
                .post(Entity.json(new OrganizationDTO(null, "Will not be created")));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void find_happyPath() throws Exception {
        when(dao.find(anyString())).thenReturn(Collections.singletonList(mock(Organization.class)));
        final Response response = resources
                .target("/organizations")
                .queryParam("filter", "something")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(dao).find(eq("something"));
    }

    @Test
    public void find_invalidFilterString_expectBadRequest() throws Exception {
        when(dao.find(anyString())).thenThrow(new IllegalArgumentException("Bad data"));
        final Response response = resources
                .target("/organizations")
                .queryParam("filter", " ")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        verify(dao).find(eq(" "));
    }

}