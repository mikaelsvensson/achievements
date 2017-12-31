package se.devscout.achievements.server.resources;

import com.auth0.jwt.algorithms.Algorithm;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.auth.CredentialsValidatorFactory;
import se.devscout.achievements.server.auth.openid.EmailIdentityProvider;
import se.devscout.achievements.server.auth.openid.GoogleIdentityProvider;
import se.devscout.achievements.server.auth.openid.MicrosoftIdentityProvider;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.resources.authenticator.JwtAuthenticator;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenIdResourceTest {

    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    //TODO: TestUtil.resourceTestRule uses another (mocked) JwtAuthenticator. This might cause bugs in future tests.
    private final OpenIdResourceAuthUtil authResourceUtil = new OpenIdResourceAuthUtil(new JwtAuthenticator(Algorithm.HMAC512("secret")), credentialsDao, peopleDao, organizationsDao, new CredentialsValidatorFactory("google_client_id"));

    private final GoogleIdentityProvider googleIdentityProvider = mock(GoogleIdentityProvider.class);
    private final MicrosoftIdentityProvider microsoftIdentityProvider = mock(MicrosoftIdentityProvider.class);
    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao, false)
            .addResource(new OpenIdResource(
                    authResourceUtil,
                    Algorithm.HMAC512("secret"),
                    googleIdentityProvider,
                    microsoftIdentityProvider,
                    new EmailIdentityProvider()))
            .build();

    public OpenIdResourceTest() throws UnsupportedEncodingException {
    }

    @Test
    public void doSignInRequest_incorrectIdp() throws Exception {
        final Response response = resources
                .target("/openid/INVALID/signin")
                .queryParam("email", "alice@example.com")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void doSignInRequest_email_validEmailAddress() throws Exception {
        final Response response = resources
                .target("/openid/password/signin")
                .queryParam("email", "alice@example.com")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT_307);
        final URI redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).endsWith("/#signin/check-mail-box");
    }

    @Test
    @Ignore(value = "Implement test")
    public void doSignInRequest_email_invalidEmailAddress() throws Exception {
        fail("Not implemented");
    }

    @Test
    public void doSignInRequest_externalIdp_noEmailAddress() throws Exception {
        when(googleIdentityProvider.getCallbackURL(anyString())).thenReturn(URI.create("http://google.example.com/"));
        when(googleIdentityProvider.getProviderAuthURL(anyString(), anyString())).thenReturn(URI.create("http://google.example.com/"));
        final Response response = resources
                .target("/openid/google/signin")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT_307);
        final URI redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).contains("google");
    }

    @Test
    @Ignore(value = "Implement test")
    public void doSignUpRequest_email_existingOrganization() throws Exception {
        //Assert that state in redirection URL contains the e-mail address
        //Assert that redirection URL leads back to localhost
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void doSignUpRequest_externalIdp_existingOrganization() throws Exception {
        //Assert that state in redirection URL contains the id of the existing organization
        //Assert that redirection URL leads to Google or Microsoft
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void doSignUpRequest_externalIdp_newOrganization() throws Exception {
        //Assert that state in redirection URL contains the name of the organization to create
        //Assert that redirection URL leads to Google or Microsoft
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignInCallback_email_correctLink() throws Exception {
        // Assert that e-mail address can be extracted from the JWT token in the link
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignInCallback_email_badLink() throws Exception {
        // Assert that no account is set up
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignInCallback_email_expiredJwtToken() throws Exception {
        // Assert that no account is set up
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignInCallback_externalIdp_validCode() throws Exception {
        // Assert that no account is set up
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignInCallback_externalIdp_invalidCode() throws Exception {
        // Assert that no account is set up
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignUpCallback_email_existingOrganization() throws Exception {
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignUpCallback_externalIdp_existingOrganization() throws Exception {
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignUpCallback_externalIdp_newOrganization() throws Exception {
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignUpCallback_expiredJwtToken() throws Exception {
        fail("Not implemented");
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignUpCallback_externalIdp_invalidCode() throws Exception {
        fail("Not implemented");
    }

}