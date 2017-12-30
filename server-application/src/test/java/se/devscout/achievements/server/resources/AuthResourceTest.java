package se.devscout.achievements.server.resources;

import com.auth0.jwt.JWT;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.api.SignupBaseDTO;
import se.devscout.achievements.server.api.SignupDTO;
import se.devscout.achievements.server.auth.SecretValidatorFactory;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.authenticator.JwtAuthenticator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

//TODO: Split into SignInResourceTest and SignUpResourceTest?
public class AuthResourceTest {

    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    private final AuthResourceUtil authResourceUtil = new AuthResourceUtil(new JwtAuthenticator("secret"), credentialsDao, peopleDao, organizationsDao, new SecretValidatorFactory("google_client_id"));
    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new OrganizationsResource(organizationsDao, mock(AchievementsDao.class), authResourceUtil))
            .addResource(new SignInResource(authResourceUtil))
            .build();

    @Test
    public void signin_happyPath() throws Exception {
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
        final Organization organization = mockOrganization("Acme Inc.");
        final Person person = mockPerson(organization, "Alice");
        final Credentials credentials = new Credentials("user", passwordValidator.getIdentityProvider(), passwordValidator.getSecret(), person);
        credentials.setId(UUID.randomUUID());
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("user"))).thenReturn(credentials);
        when(credentialsDao.read(eq(credentials.getId()))).thenReturn(credentials);

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
    public void signup_noOrganization_expectBadRequest() throws Exception {
        final Response response = resources
                .target("/organizations/signup")
                .request()
                .post(Entity.json(new SignupDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(organizationsDao, never()).read(any(UUID.class));
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(credentialsDao, never()).create(any(Person.class), any(CredentialsProperties.class));
    }

    @Test
    public void signup_newOrganization_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        when(organizationsDao.create(any(OrganizationProperties.class))).thenReturn(org);
        when(organizationsDao.find(anyString())).thenReturn(Collections.EMPTY_LIST);
        when(peopleDao.create(any(Organization.class), any(PersonProperties.class))).thenAnswer(invocation -> mockPerson(
                invocation.getArgument(0),
                ((PersonProperties) invocation.getArgument(1)).getName(),
                ((PersonProperties) invocation.getArgument(1)).getEmail()));
        final Response response = resources
                .target("organizations/signup")
                .request()
                .post(Entity.json(new SignupDTO("alice@example.com", "password", org.getName(), IdentityProvider.PASSWORD)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final AuthTokenDTO responseDTO = response.readEntity(AuthTokenDTO.class);
        assertThat(responseDTO.token).isNotEmpty();

        verify(organizationsDao).find(anyString());
        verify(organizationsDao).create(any(OrganizationProperties.class));
        verify(peopleDao).create(eq(org), any(PersonProperties.class));
        verify(credentialsDao).create(any(Person.class), any(CredentialsProperties.class));
    }

    @Test
    public void signup_existingOrganization_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "alice", "alice@example.com");
        when(organizationsDao.read(eq(org.getId()))).thenReturn(org);
        when(peopleDao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);
        when(peopleDao.getByEmail(eq(person.getEmail()))).thenReturn(Collections.singletonList(person));
        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/signup")
                .request()
                .post(Entity.json(new SignupBaseDTO("alice@example.com", "password", IdentityProvider.PASSWORD)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final AuthTokenDTO responseDTO = response.readEntity(AuthTokenDTO.class);
        assertThat(responseDTO.token).isNotEmpty();

        verify(organizationsDao).read(eq(org.getId()));
        verify(organizationsDao, never()).find(anyString());
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(peopleDao).getByEmail(eq(person.getEmail()));
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
    }

    @Test
    public void signup_tryToCreateAlreadyExistingOrganization_expectConflict() throws Exception {
        final Organization org = mockOrganization("org");
        when(organizationsDao.find(eq("org"))).thenReturn(Collections.singletonList(org));
        final Response response = resources
                .target("/organizations/signup/")
                .request()
                .post(Entity.json(new SignupDTO("password", "alice@example.com", org.getName(), IdentityProvider.PASSWORD)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT_409);

        verify(organizationsDao, never()).read(any(UUID.class));
        verify(organizationsDao).find(eq("org"));
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(credentialsDao, never()).create(any(Person.class), any(CredentialsProperties.class));
    }

    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException {
        return mockPerson(org, name, null);
    }

    private Person mockPerson(Organization org, String name, String email) throws ObjectNotFoundException {
        final Integer uuid = getRandomNonZeroValue();

        final Person person = mock(Person.class);
        when(person.getId()).thenReturn(uuid);
        when(person.getOrganization()).thenReturn(org);
        when(person.getName()).thenReturn(name);
        when(person.getEmail()).thenReturn(email);

        when(peopleDao.read(eq(uuid))).thenReturn(person);

        return person;
    }

    private int getRandomNonZeroValue() {
        return new Random().nextInt(10000) + 1;
    }

    private Organization mockOrganization(String name) throws ObjectNotFoundException {
        final UUID uuid = UUID.randomUUID();

        final Organization orgA = mock(Organization.class);
        when(orgA.getId()).thenReturn(uuid);
        when(orgA.getName()).thenReturn(name);

        when(organizationsDao.read(eq(uuid))).thenReturn(orgA);

        return orgA;
    }

}