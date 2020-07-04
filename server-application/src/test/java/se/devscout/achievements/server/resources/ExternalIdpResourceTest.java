package se.devscout.achievements.server.resources;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.I18n;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.*;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.resources.auth.ExternalIdpResource;
import se.devscout.achievements.server.resources.exceptionhandling.CallbackResourceExceptionMapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static se.devscout.achievements.server.MockUtil.*;

public class ExternalIdpResourceTest {

    private final JwtTokenService tokenService = new JwtTokenServiceImpl("secret");
    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final I18n i18n = mock(I18n.class);
//    private HttpServletRequest req = mock(HttpServletRequest.class);
//    private HttpServletResponse resp = mock(HttpServletResponse.class);

    private final IdentityProvider identityProvider = mock(IdentityProvider.class);

    private final JwtSignUpTokenService signUpTokenService = new JwtSignUpTokenService(tokenService);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao, false)
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addResource(new ExternalIdpResource(
                    ImmutableMap.of("provider", identityProvider),
                    credentialsDao,
                    peopleDao,
                    organizationsDao,
                    URI.create("http://gui"),
                    URI.create("http://server"),
                    new JwtSignInTokenService(tokenService),
                    signUpTokenService,
                    emailSender,
                    i18n))
            .addProvider(new CallbackResourceExceptionMapper(URI.create("http://gui")))
            .build();

    @Before
    public void setUp() throws Exception {
        setupDefaultCredentials(credentialsDao);
    }

    @Test
    @Ignore(value = "Fix test")
    public void doSignInRequest_incorrectIdp() throws Exception {
        final var response = resources
                .target("/auth/INVALID/signin")
                .queryParam("email", "alice@example.com")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT_307);
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).isEqualTo("http://gui/#signin-failed/system-error");
    }

    @Test
    public void doSignInRequest_email_validEmailAddress() throws Exception {
        when(
                identityProvider.getRedirectUri(any(HttpServletRequest.class), any(HttpServletResponse.class), anyString(), any(URI.class))
        ).thenReturn(
                URI.create("http://gui/#signin/check-mail-box")
        );
        final var data = new MultivaluedStringMap();
        data.putSingle("key", "value");
        final var response = resources
                .target("/auth/provider/signin")
                .request()
                .post(Entity.form(data));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SEE_OTHER_303);
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).isEqualTo("http://gui/#signin/check-mail-box");
    }

    @Test
    @Ignore(value = "Implement test")
    public void doSignInRequest_email_invalidEmailAddress() throws Exception {
        fail("Not implemented");
    }

    @Test
    public void doSignInRequest_externalIdp_noEmailAddress() throws Exception {
        when(identityProvider.getRedirectUri(any(HttpServletRequest.class), any(HttpServletResponse.class), anyString(), any(URI.class))).thenAnswer(invocation -> invocation.getArgument(3));
        final var response = resources
                .target("/auth/provider/signin")
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SEE_OTHER_303);
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).isEqualTo("http://server/api/auth/provider/signin/callback");
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
    public void handleSignInCallback_externalIdp_validCodeExistingCredentials() throws Exception {
        when(identityProvider.handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(new ValidationResult(USERNAME_READER, USERNAME_READER, true, CredentialsType.PASSWORD, null));

        final var response = resources
                .target("/auth/provider/signin/callback")
                .queryParam("code", "the_state")
                .request()
                .get();

        verify(identityProvider).handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SEE_OTHER_303);

        // Assert that user is redirected to a URL which appears to include a JWT
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).matches("http://gui/#signin/[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");

        // Assert that no account is set up
        verify(peopleDao, never()).create(any(), any());
        verify(peopleDao, never()).update(any(), any());
        verify(credentialsDao, never()).create(any(), any());
        verify(credentialsDao, never()).update(any(), any());
        verify(organizationsDao, never()).create(any());
        verify(organizationsDao, never()).update(any(), any());
    }

    @Test
    public void handleSignInCallback_handleCallback_failure() throws Exception {
        when(identityProvider.handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenThrow(new IdentityProviderException("Something went wrong"));

        final var response = resources
                .target("/auth/provider/signin/callback")
                .queryParam("code", "the_state")
                .request()
                .get();

        verify(identityProvider).handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SEE_OTHER_303);

        // Assert that user is redirected to a URL which appears to include a JWT
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).isEqualTo("http://gui/#signin-failed/unspecified");

        // Assert that no account is set up
        verify(peopleDao, never()).create(any(), any());
        verify(peopleDao, never()).update(any(), any());
        verify(credentialsDao, never()).create(any(), any());
        verify(credentialsDao, never()).update(any(), any());
        verify(organizationsDao, never()).create(any());
        verify(organizationsDao, never()).update(any(), any());
    }

    @Test
    public void handleSignInCallback_existingPersonWithoutCredentials_happyPath() throws Exception {
        final var email = "person-without-credentials@example.com";
        final var organization = mockOrganization("Acme Inc.");
        final var person = mockPerson(organization, "Alice Reader", "alice_reader", Roles.READER);
        final var credentials = mockCredentials(person, email);
        when(identityProvider.handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(new ValidationResult(email, email, true, CredentialsType.PASSWORD, null));

        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(email))).thenThrow(new ObjectNotFoundException());

        when(credentialsDao.create(eq(person), any(CredentialsProperties.class))).thenReturn(credentials);

        when(peopleDao.getByEmail(eq(email))).thenReturn(Collections.singletonList(person));

        final var response = resources
                .target("/auth/provider/signin/callback")
                .queryParam("code", "the_state")
                .request()
                .get();

        verify(identityProvider).handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(credentialsDao).get(eq(CredentialsType.PASSWORD), eq(email));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SEE_OTHER_303);

        // Assert that user is redirected to a URL which appears to include a JWT
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).matches("http://gui/#signin/[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");

        verify(peopleDao, never()).create(any(), any());
        verify(peopleDao, never()).update(any(), any());
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
        verify(credentialsDao, never()).update(any(), any());
        verify(organizationsDao, never()).create(any());
        verify(organizationsDao, never()).update(any(), any());
    }

    @Test
    @Ignore(value = "Implement test")
    public void handleSignUpCallback_email_existingOrganization() throws Exception {
        fail("Not implemented");
    }

    /*
     * Use case:
     * - Alice signs up (adds herself to the system) after getting a sign-up link
     *   specific to her organization.
     *
     * Preconditions:
     * - Acme exists as organization object.
     * - Alice exists as person object in Acme.
     * - Alice's person object has Alice's e-mail address.
     * - Alice's person object has NO credentials.
     *
     * Verify:
     * - A credentials object is created for Alice when signing in using an
     *   identity provider which authenticates Alice's e-mail address
     */
    @Test
    public void handleSignUpCallback_externalIdp_existingSpecificOrganizationExistingPerson() throws Exception {
        final var email = "alice@example.com";

        final var organization = mockOrganization("Acme");
        final var person = mockPerson(organization, "Alice", null, email, Roles.READER);
        final var credentials = mockCredentials(person, email);

        final var callbackState = signUpTokenService.encode(new JwtSignUpToken(new UuidString(organization.getId()), null));

        when(identityProvider.handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(new ValidationResult(email, email, true, CredentialsType.PASSWORD, null, callbackState));
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(email))).thenThrow(new ObjectNotFoundException());
        when(credentialsDao.create(eq(person), any(CredentialsProperties.class))).thenReturn(credentials);
        when(peopleDao.getByEmail(eq(email))).thenReturn(Collections.singletonList(person));
        when(organizationsDao.read(eq(organization.getId()))).thenReturn(organization);

        final var response = resources
                .target("/auth/provider/signup/callback")
                .queryParam("code", "the_provider_auth_code")
                .queryParam("state", callbackState)
                .request()
                .get();

        // Assert that user is redirected to a URL which appears to include a JWT
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).matches("http://gui/#signin/[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");

        verify(peopleDao, never()).create(any(), any());
        verify(peopleDao, never()).update(any(), any());
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
        verify(credentialsDao, never()).update(any(), any());
        verify(organizationsDao, never()).create(any());
        verify(organizationsDao, never()).update(any(), any());
    }

    /*
     * Use case:
     * - Alice signs up (adds herself to the system) after getting a sign-up link
     *   specific to her organization.
     *
     * Preconditions:
     * - Acme exists as organization object.
     * - Alice DOES NOT exist as person object in Acme.
     *
     * Verify:
     * - Person object AND credentials object are created for Alice when signing
     *   in using an identity provider which authenticates Alice's e-mail address.
     */
    @Test
    public void handleSignUpCallback_externalIdp_existingSpecificOrganizationNewPerson() throws Exception {

        final var email = "alice@example.com";

        final var organization = mockOrganization("Acme");
        final var person = mockPerson(organization, "Alice", null, email, Roles.READER);
        final var credentials = mockCredentials(person, email);

        final var callbackState = signUpTokenService.encode(new JwtSignUpToken(new UuidString(organization.getId()), null));

        when(identityProvider.handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(new ValidationResult(email, email, true, CredentialsType.PASSWORD, null, callbackState));
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(email))).thenThrow(new ObjectNotFoundException());
        when(credentialsDao.create(eq(person), any(CredentialsProperties.class))).thenReturn(credentials);
        when(peopleDao.getByEmail(eq(email))).thenReturn(Collections.emptyList());
        when(peopleDao.create(eq(organization), any(PersonProperties.class))).thenReturn(person);
        when(organizationsDao.read(eq(organization.getId()))).thenReturn(organization);

        final var response = resources
                .target("/auth/provider/signup/callback")
                .queryParam("code", "the_provider_auth_code")
                .queryParam("state", callbackState)
                .request()
                .get();

        // Assert that user is redirected to a URL which appears to include a JWT
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).matches("http://gui/#signin/[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");

        verify(peopleDao).create(eq(organization), any(PersonProperties.class));
        verify(peopleDao, never()).update(any(), any());
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
        verify(credentialsDao, never()).update(any(), any());
        verify(organizationsDao, never()).create(any());
        verify(organizationsDao, never()).update(any(), any());
    }

    /*
     * Use case:
     * - Alice signs up (signs in for the first time) after hearing that someone
     *   added her to the system but without having a sign-up link specific to her
     *   organization.
     *
     * Preconditions:
     * - Acme exists as organization object.
     * - Alice exists as person object in Acme.
     * - Alice's person object has Alice's e-mail address.
     * - Alice's person object has NO credentials.
     *
     * Verify:
     * - A credentials object is created for Alice when signing in using an
     *   identity provider which authenticates Alice's e-mail address
     */
    @Test
    public void handleSignUpCallback_externalIdp_existingUnspecifiedOrganizationExistingPerson() throws Exception {
        final var email = "alice@example.com";

        final var organization = mockOrganization("Acme");
        final var person = mockPerson(organization, "Alice", null, email, Roles.READER);
        final var credentials = mockCredentials(person, email);

        final var callbackState = signUpTokenService.encode(new JwtSignUpToken(null, null));

        when(identityProvider.handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(new ValidationResult(email, email, true, CredentialsType.PASSWORD, null, callbackState));
        when(credentialsDao.create(eq(person), any(CredentialsProperties.class))).thenReturn(credentials);
        when(peopleDao.getByEmail(eq(email))).thenReturn(Collections.singletonList(person));

        final var response = resources
                .target("/auth/provider/signup/callback")
                .queryParam("code", "the_provider_auth_code")
                .queryParam("state", callbackState)
                .request()
                .get();

        // Assert that user is redirected to a URL which appears to include a JWT
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).matches("http://gui/#signin/[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");

        verify(peopleDao, never()).create(any(), any());
        verify(peopleDao, never()).update(any(), any());
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
        verify(credentialsDao, never()).update(any(), any());
        verify(organizationsDao, never()).create(any());
        verify(organizationsDao, never()).update(any(), any());
    }

    /*
     * Use case:
     * - Alice wants to create a new organization, with herself as the first member.
     *
     * Preconditions:
     * - Acme DOES NOT exists as organization object.
     * - Alice DOES NOT exist as person object in Acme.
     *
     * Verify:
     * - Organization object AND person object AND credentials object are created when signing
     *   in using an identity provider which authenticates Alice's e-mail address.
     */
    @Test
    public void handleSignUpCallback_externalIdp_newOrganization() throws Exception {
        final var email = "alice@example.com";

        final var organization = mockOrganization("Acme");
        final var person = mockPerson(organization, "Alice", null, email, Roles.READER);
        final var credentials = mockCredentials(person, email);

        final var callbackState = signUpTokenService.encode(new JwtSignUpToken(null, "Acme"));

        when(identityProvider.handleCallback(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(new ValidationResult(email, email, true, CredentialsType.PASSWORD, null, callbackState));
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(email))).thenThrow(new ObjectNotFoundException());
        when(credentialsDao.create(eq(person), any(CredentialsProperties.class))).thenReturn(credentials);
        when(peopleDao.getByEmail(eq(email))).thenReturn(Collections.emptyList());
        when(peopleDao.create(eq(organization), any(PersonProperties.class))).thenReturn(person);
        when(organizationsDao.find(eq("Acme"))).thenReturn(Collections.emptyList());
        when(organizationsDao.create(any(OrganizationProperties.class))).thenReturn(organization);

        final var response = resources
                .target("/auth/provider/signup/callback")
                .queryParam("code", "the_provider_auth_code")
                .queryParam("state", callbackState)
                .request()
                .get();

        // Assert that user is redirected to a URL which appears to include a JWT
        final var redirectURI = URI.create(response.getHeaderString("Location"));
        assertThat(redirectURI.toString()).matches("http://gui/#signin/[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");

        verify(peopleDao).create(eq(organization), any(PersonProperties.class));
        verify(peopleDao, never()).update(any(), any());
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
        verify(credentialsDao, never()).update(any(), any());
        verify(organizationsDao).create(any(OrganizationProperties.class));
        verify(organizationsDao, never()).update(any(), any());
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