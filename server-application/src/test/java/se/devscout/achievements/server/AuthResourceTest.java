package se.devscout.achievements.server;

import com.auth0.jwt.JWT;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.JwtAuthenticator;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.AuthResource;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static se.devscout.achievements.server.MockUtil.mockOrganization;
import static se.devscout.achievements.server.MockUtil.mockPerson;

public class AuthResourceTest {

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    private final PeopleDao peopleDao = mock(PeopleDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao, peopleDao)
            .addResource(new AuthResource(new JwtAuthenticator("secret"), credentialsDao))
            .build();

    @Before
    public void setUp() throws Exception {
        final Credentials credentials = new Credentials("username", new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray()));
        final Organization org = mockOrganization("The Company");
        final Person user = mockPerson(org, "The User");
        credentials.setPerson(user);
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("user"))).thenReturn(credentials);
    }

    @Test
    public void create_happyPath() throws Exception {

        final Response response = resources
                .target("/auth/token")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final AuthTokenDTO dto = response.readEntity(AuthTokenDTO.class);

        assertThat(dto.token).isNotEmpty();
        JWT.decode(dto.token);
    }

}