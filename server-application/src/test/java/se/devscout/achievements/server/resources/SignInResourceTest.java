package se.devscout.achievements.server.resources;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.CredentialsValidatorFactory;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.resources.authenticator.JwtAuthenticator;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SignInResourceTest {

    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    //TODO: TestUtil.resourceTestRule uses another (mocked) JwtAuthenticator. This might cause bugs in future tests.
    private final OpenIdResourceAuthUtil authResourceUtil = new OpenIdResourceAuthUtil(new JwtAuthenticator(Algorithm.HMAC512("secret")), credentialsDao, peopleDao, organizationsDao, new CredentialsValidatorFactory("google_client_id"));

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new SignInResource(authResourceUtil))
            .build();

    public SignInResourceTest() throws UnsupportedEncodingException {
    }

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
    }

    @Test
    public void signIn_happyPath() throws Exception {
        final Response response = resources
                .target("/signin")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final AuthTokenDTO dto = response.readEntity(AuthTokenDTO.class);

        assertThat(dto.token).isNotEmpty();
        JWT.decode(dto.token);
    }

    @Test
    public void signIn_badPassword_expect401() throws Exception {
        final Response response = resources
                .target("/signin")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:invalid_password".getBytes(Charsets.UTF_8)))
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(response.readEntity(String.class)).isEqualTo("Credentials are required to access this resource.");
    }

    @Test
    public void signIn_wrongUser_expect401() throws Exception {
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq("missing_user"))).thenThrow(new ObjectNotFoundException());
        final Response response = resources
                .target("/signin")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("missing_user:password".getBytes(Charsets.UTF_8)))
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