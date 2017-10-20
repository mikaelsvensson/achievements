package se.devscout.achievements.server;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.SignupDTO;
import se.devscout.achievements.server.api.SignupResponseDTO;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.SignupResource;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SignupResourceTest {

    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new SignupResource(peopleDao, organizationsDao, credentialsDao))
            .build();

    @Test
    public void signup_noOrganization_expectBadRequest() throws Exception {
        final Response response = resources
                .target("/signup/")
                .request()
                .post(Entity.json(new SignupDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(organizationsDao, never()).read(any(UUID.class));
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(credentialsDao, never()).create(any(Person.class), any(CredentialsProperties.class));
    }

    @Test
    public void signup_missingOrganization_expectBadRequest() throws Exception {
        when(organizationsDao.read(any(UUID.class))).thenThrow(new ObjectNotFoundException());
        final Response response = resources
                .target("/signup/")
                .request()
                .post(Entity.json(new SignupDTO(UUID.randomUUID(), "alice", "password")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(organizationsDao).read(any(UUID.class));
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(credentialsDao, never()).create(any(Person.class), any(CredentialsProperties.class));
    }

    @Test
    public void signup_existingOrganization_happyPath() throws Exception {
        final Organization org = mockOrganization("The Organization");
        final Person person = mockPerson(org, "Alice");
        when(organizationsDao.read(any(UUID.class))).thenReturn(org);
        when(peopleDao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);
        final Response response = resources
                .target("/signup/")
                .request()
                .post(Entity.json(new SignupDTO(org.getId(), "Alice", "password")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        assertThat(response.getLocation()).hasPath("/organizations/" + org.getId() + "/people/" + person.getId());
        final SignupResponseDTO actualResponse = response.readEntity(SignupResponseDTO.class);
        assertThat(actualResponse.organization.id).isEqualTo(org.getId().toString());
        assertThat(actualResponse.organization.name).isEqualTo("The Organization");
        assertThat(actualResponse.person.id).isEqualTo(person.getId().toString());
        assertThat(actualResponse.person.name).isEqualTo("Alice");

        verify(organizationsDao).read(any(UUID.class));
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao).create(any(Organization.class), any(PersonProperties.class));
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
    }

    @Test
    public void signup_tryToCreateAlreadyExistingOrganization_expectConflict() throws Exception {
        final Organization org = mockOrganization("org");
        when(organizationsDao.find(eq("org"))).thenReturn(Collections.singletonList(org));
        final Response response = resources
                .target("/signup/")
                .request()
                .post(Entity.json(new SignupDTO("org", "alice", "password")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT_409);

        verify(organizationsDao, never()).read(any(UUID.class));
        verify(organizationsDao).find(eq("org"));
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(credentialsDao, never()).create(any(Person.class), any(CredentialsProperties.class));
    }

    @Test
    public void signup_newUser_expectConflict() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "alice");
        when(organizationsDao.read(any(UUID.class))).thenReturn(org);
        when(peopleDao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);
        final Response response = resources
                .target("/signup/")
                .request()
                .post(Entity.json(new SignupDTO(UUID.randomUUID(), "alice", "password")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        verify(organizationsDao).read(any(UUID.class));
        verify(organizationsDao, never()).find(anyString());
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao).create(eq(org), any(PersonProperties.class));
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
    }

    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException {
        final Integer uuid = getRandomNonZeroValue();

        final Person person = mock(Person.class);
        when(person.getId()).thenReturn(uuid);
        when(person.getOrganization()).thenReturn(org);
        when(person.getName()).thenReturn(name);

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