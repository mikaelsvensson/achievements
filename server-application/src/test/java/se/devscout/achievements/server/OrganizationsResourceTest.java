package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.TooManyOrganizationsException;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.OrganizationsResource;
import se.devscout.achievements.server.auth.User;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class OrganizationsResourceTest {

    private final OrganizationsDao dao = mock(OrganizationsDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
//            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addProvider(AchievementsApplication.createAuthFeature(mock(SessionFactory.class), credentialsDao))
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthValueFactoryProvider.Binder<>(User.class))

            .addResource(new OrganizationsResource(dao))
            .build();

    @Before
    public void setUp() throws Exception {
        final Credentials credentials = new Credentials("username", new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray()));
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("user"))).thenReturn(credentials);
    }

    @Test
    public void get_happyPath() throws Exception {
        final UUID uuid = UUID.randomUUID();
        when(dao.read(eq(uuid))).thenReturn(new Organization(uuid, "Alice's Organization"));
        final OrganizationDTO dto = request("/organizations/" + uuid.toString()).get(OrganizationDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.name).isEqualTo("Alice's Organization");
    }

    @Test
    public void create_happyPath() throws Exception {
        when(dao.create(any(OrganizationProperties.class))).thenAnswer(invocation -> new Organization(UUID.randomUUID(), ((OrganizationProperties) invocation.getArgument(0)).getName()));
        final Response response = request("/organizations").post(Entity.json(new OrganizationDTO(null, "Bob's Club")));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final OrganizationDTO dto = response.readEntity(OrganizationDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.name).isEqualTo("Bob's Club");
    }

    private Invocation.Builder request(String path) {
        return request(path, Collections.EMPTY_MAP);
    }

    private Invocation.Builder request(String path, Map<String, String> queryParameters) {
        WebTarget webTarget = resources.target(path);
        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            webTarget = webTarget.queryParam(entry.getKey(), entry.getValue());
        }
        ;
        return webTarget
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)));
    }

    @Test
    public void create_tooManyOrganizations() throws Exception {
        when(dao.create(any(OrganizationProperties.class))).thenThrow(new TooManyOrganizationsException());
        final Response response = request("/organizations").post(Entity.json(new OrganizationDTO(null, "Will not be created")));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void find_happyPath() throws Exception {
        when(dao.find(anyString())).thenReturn(Collections.singletonList(mock(Organization.class)));
        final Response response = request("/organizations", Collections.singletonMap("filter", "something")).get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(dao).find(eq("something"));
    }

    @Test
    public void find_invalidFilterString_expectBadRequest() throws Exception {
        when(dao.find(anyString())).thenThrow(new IllegalArgumentException("Bad data"));
        final Response response = request("/organizations", Collections.singletonMap("filter", " ")).get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        verify(dao).find(eq(" "));
    }

}