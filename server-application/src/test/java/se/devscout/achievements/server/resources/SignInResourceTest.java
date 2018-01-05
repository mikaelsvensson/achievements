package se.devscout.achievements.server.resources;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceImpl;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.resources.authenticator.JwtAuthenticator;
import se.devscout.achievements.server.resources.authenticator.TokenGenerator;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SignInResourceTest {

    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    private final JwtTokenService tokenService = new JwtTokenServiceImpl("secret");

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new SignInResource(new TokenGenerator(new JwtAuthenticator(tokenService), credentialsDao)))
            .build();

    public SignInResourceTest() {
    }

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
    }

    @Test
    public void signIn_happyPath() throws Exception {
        final Response response = resources
                .target("/signin")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final AuthTokenDTO dto = response.readEntity(AuthTokenDTO.class);

        assertThat(dto.token).isNotEmpty();
        final DecodedJWT actual = tokenService.decode(dto.token);
        assertThat(actual.getSubject()).isEqualTo("Alice Editor");
    }

    @Test
    public void signIn_badPassword_expect401() throws Exception {
        final Response response = resources
                .target("/signin")
                .register(HttpAuthenticationFeature.basic(MockUtil.USERNAME_READER, "invalid_password"))
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(response.readEntity(String.class)).isEqualTo("Credentials are required to access this resource.");
    }

    @Test
    public void signIn_wrongUser_expect401() throws Exception {
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq("missing_user"))).thenThrow(new ObjectNotFoundException());
        final Response response = resources
                .target("/signin")
                .register(HttpAuthenticationFeature.basic("missing_user", "password"))
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(response.readEntity(String.class)).isEqualTo("Credentials are required to access this resource.");
    }

    @Test
    public void signIn_noUser_expect401() throws Exception {
        final Response response = resources
                .target("/signin")
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(response.readEntity(String.class)).isEqualTo("Credentials are required to access this resource.");
    }

}