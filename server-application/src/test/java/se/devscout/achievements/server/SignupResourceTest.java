package se.devscout.achievements.server;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.SignupBaseDTO;
import se.devscout.achievements.server.api.SignupDTO;
import se.devscout.achievements.server.api.SignupResponseDTO;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.OrganizationsResource;
import se.devscout.achievements.server.resources.SignupResource;
import se.devscout.achievements.server.resources.UuidString;

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
    public void signup_newOrganization_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "alice");
        when(organizationsDao.create(any(OrganizationProperties.class))).thenReturn(org);
        when(organizationsDao.find(anyString())).thenReturn(Collections.EMPTY_LIST);
        when(peopleDao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);
        final Response response = resources
                .target("/signup/")
                .request()
                .post(Entity.json(new SignupDTO("alice", "password", "alice@example.com", org.getName())));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final SignupResponseDTO responseDTO = response.readEntity(SignupResponseDTO.class);
        assertThat(responseDTO.organization.id).isEqualTo(UuidString.toString(org.getId()));
        assertThat(responseDTO.organization.name).isEqualTo(org.getName());
        assertThat(responseDTO.person.id).isEqualTo(person.getId());

        verify(organizationsDao).find(anyString());
        verify(organizationsDao).create(any(OrganizationProperties.class));
        verify(peopleDao).create(eq(org), any(PersonProperties.class));
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
    }

    @Test
    public void signup_existingOrganization_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "alice");
        when(organizationsDao.read(eq(org.getId()))).thenReturn(org);
        when(peopleDao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);
        final Response response = resources
                .target("/signup/" + UuidString.toString(org.getId()))
                .request()
                .post(Entity.json(new SignupBaseDTO("alice", "password", "alice@example.com")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);

        final SignupResponseDTO responseDTO = response.readEntity(SignupResponseDTO.class);
        assertThat(responseDTO.organization.id).isEqualTo(UuidString.toString(org.getId()));
        assertThat(responseDTO.organization.name).isEqualTo("org");
        assertThat(responseDTO.person.id).isEqualTo(person.getId());

        verify(organizationsDao).read(eq(org.getId()));
        verify(organizationsDao, never()).find(anyString());
        verify(organizationsDao, never()).create(any(OrganizationProperties.class));
        verify(peopleDao).create(eq(org), any(PersonProperties.class));
        verify(credentialsDao).create(eq(person), any(CredentialsProperties.class));
    }

    @Test
    public void signup_tryToCreateAlreadyExistingOrganization_expectConflict() throws Exception {
        final Organization org = mockOrganization("org");
        when(organizationsDao.find(eq("org"))).thenReturn(Collections.singletonList(org));
        final Response response = resources
                .target("/signup/")
                .request()
                .post(Entity.json(new SignupDTO( "alice", "password", "alice@example.com", org.getName())));

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