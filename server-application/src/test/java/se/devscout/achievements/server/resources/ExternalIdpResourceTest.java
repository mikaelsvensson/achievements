package se.devscout.achievements.server.resources;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtSignUpTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceImpl;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.resources.auth.ExternalIdpResource;

import javax.ws.rs.core.Response;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExternalIdpResourceTest {

    private final JwtTokenService tokenService = new JwtTokenServiceImpl("secret");
    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    private final IdentityProvider identityProvider = mock(IdentityProvider.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao, false)
            .addResource(new ExternalIdpResource(
                    ImmutableMap.of("provider", identityProvider),
                    credentialsDao,
                    peopleDao,
                    organizationsDao,
                    URI.create("http://gui"),
                    URI.create("http://server"),
                    new JwtSignInTokenService(tokenService),
                    new JwtSignUpTokenService(tokenService)))
            .build();

    @Test
    @Ignore(value = "Fix test")
    public void doSignInRequest_incorrectIdp() throws Exception {
        final Response response = resources
                .target("/openid/INVALID/signin")
                .queryParam("email", "alice@example.com")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT_307);
        final URI redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).isEqualTo("http://gui/#signin-failed/system-error");
    }

    @Test
    @Ignore(value = "Fix test")
    public void doSignInRequest_email_validEmailAddress() throws Exception {
        final Response response = resources
                .target("/openid/provider/signin")
                .queryParam("email", "alice@example.com")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT_307);
        final URI redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).isEqualTo("http://gui/#signin/check-mail-box");
    }

    @Test
    @Ignore(value = "Implement test")
    public void doSignInRequest_email_invalidEmailAddress() throws Exception {
        fail("Not implemented");
    }

    @Test
    public void doSignInRequest_externalIdp_noEmailAddress() throws Exception {
        when(identityProvider.getRedirectUri(anyString(), any(URI.class), anyMap())).thenAnswer(invocation -> invocation.getArgument(1));
        final Response response = resources
                .target("/openid/provider/signin")
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SEE_OTHER_303);
        final URI redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).isEqualTo("http://server/api/openid/provider/signin/callback");

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