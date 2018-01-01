package se.devscout.achievements.server.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.api.SignupBaseDTO;
import se.devscout.achievements.server.api.SignupDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OrganizationsSignUpResourceTest {

    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    //TODO: TestUtil.resourceTestRule uses another (mocked) JwtAuthenticator. This might cause bugs in future tests.
//    private final AuthResourceUtil authResourceUtil = new AuthResourceUtil(new JwtAuthenticator(Algorithm.HMAC512("secret")), credentialsDao, peopleDao, organizationsDao, new CredentialsValidatorFactory("google_client_id"));

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new OrganizationsResource(organizationsDao, mock(AchievementsDao.class)/*, authResourceUtil*/))
            .build();

    public OrganizationsSignUpResourceTest() throws UnsupportedEncodingException {
    }

    @Test
    @Ignore(value = "Ignored test should be reimplemented in OpenIdResourceTest")
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
    @Ignore(value = "Ignored test should be reimplemented in OpenIdResourceTest")
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
                .post(Entity.json(new SignupDTO("alice@example.com", "password", org.getName(), CredentialsType.PASSWORD)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final AuthTokenDTO responseDTO = response.readEntity(AuthTokenDTO.class);
        assertThat(responseDTO.token).isNotEmpty();

        verify(organizationsDao).find(anyString());
        verify(organizationsDao).create(any(OrganizationProperties.class));
        verify(peopleDao).create(eq(org), any(PersonProperties.class));
        verify(credentialsDao).create(any(Person.class), any(CredentialsProperties.class));
    }

    @Test
    @Ignore(value = "Ignored test should be reimplemented in OpenIdResourceTest")
    public void signup_existingOrganization_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "alice", "alice@example.com");
        when(peopleDao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);
        when(peopleDao.getByEmail(eq(person.getEmail()))).thenReturn(Collections.singletonList(person));
        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/signup")
                .request()
                .post(Entity.json(new SignupBaseDTO("alice@example.com", "password", CredentialsType.PASSWORD)));

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
    @Ignore(value = "Ignored test should be reimplemented in OpenIdResourceTest")
    public void signup_tryToCreateAlreadyExistingOrganization_expectConflict() throws Exception {
        final Organization org = mockOrganization("org");
        when(organizationsDao.find(eq("org"))).thenReturn(Collections.singletonList(org));
        final Response response = resources
                .target("/organizations/signup/")
                .request()
                .post(Entity.json(new SignupDTO("password", "alice@example.com", org.getName(), CredentialsType.PASSWORD)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT_409);

        verify(organizationsDao, never()).read(any(UUID.class));
        verify(organizationsDao).find(eq("org"));
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(credentialsDao, never()).create(any(Person.class), any(CredentialsProperties.class));
    }

    private Person mockPerson(Organization org, String name, String email) throws ObjectNotFoundException {
        final Person person = MockUtil.mockPerson(org, name, null, email, Roles.READER);

        when(peopleDao.read(eq(person.getId()))).thenReturn(person);

        return person;
    }

    private Organization mockOrganization(String name) throws ObjectNotFoundException {
        final Organization org = MockUtil.mockOrganization(name);

        when(organizationsDao.read(eq(org.getId()))).thenReturn(org);

        return org;
    }
}