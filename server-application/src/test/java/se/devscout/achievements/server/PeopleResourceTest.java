package se.devscout.achievements.server;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;
import se.devscout.achievements.server.resources.PeopleResource;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class PeopleResourceTest {

    private final PeopleDao dao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new PeopleResource(dao, organizationsDao))
            .build();

    @Test
    public void get_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "Alice");

        final Response response = resources
                .target("/organizations/" + org.getId() + "/people/" + person.getId())
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final PersonDTO dto = response.readEntity(PersonDTO.class);
        assertThat(dto.id).isEqualTo(person.getId().toString());

        verify(dao).get(eq(person.getId().toString()));
    }

    @Test
    public void get_notFound() throws Exception {
        when(dao.get(eq("PERSON_ID"))).thenThrow(new NotFoundException());
        final Response response = resources
                .target("/organizations/ORG_ID/people/PERSON_ID")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao).get(eq("PERSON_ID"));
    }

    @Test
    public void delete_notFound() throws Exception {
        final Organization org = mockOrganization("Org");

        doThrow(new NotFoundException()).when(dao).get(eq("PERSON_ID"));

        final Response response = resources
                .target("/organizations/ORG_ID/people/PERSON_ID")
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao, never()).delete(anyString());
    }

    @Test
    public void delete_happyPath() throws Exception {
        final Organization org = mockOrganization("Org");
        final Person person = mockPerson(org, "name");

        final Response response = resources
                .target("/organizations/" + org.getId() + "/people/" + person.getId())
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(dao).delete(eq(person.getId().toString()));
    }

    @Test
    public void delete_wrongOrganization_expectNotFound() throws Exception {

        final Organization orgA = mockOrganization("ORG_A");
        final Organization orgB = mockOrganization("ORG_B");
        final Person person = mockPerson(orgA, "name");

        final Response response = resources
                .target("/organizations/" + orgB.getId() + "/people/" + person.getId())
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(organizationsDao).get(eq(orgB.getId().toString()));
        verify(dao, never()).delete(anyString());
    }

    @Test
    public void get_wrongOrganization_expectNotFound() throws Exception {

        final Organization orgA = mockOrganization("ORG_A");
        final Organization orgB = mockOrganization("ORG_B");
        final Person person = mockPerson(orgA, "name");

        final Response response = resources
                .target("/organizations/" + orgB.getId() + "/people/" + person.getId())
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(organizationsDao).get(eq(orgB.getId().toString()));
        verify(dao).get(eq(person.getId().toString()));
    }

    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException {
        final UUID uuid = UUID.randomUUID();

        final Person person = mock(Person.class);
        when(person.getId()).thenReturn(uuid);
        when(person.getOrganization()).thenReturn(org);
        when(person.getName()).thenReturn(name);

        when(dao.get(eq(uuid.toString()))).thenReturn(person);

        return person;
    }

    private Organization mockOrganization(String name) throws ObjectNotFoundException {
        final UUID uuid = UUID.randomUUID();

        final Organization orgA = mock(Organization.class);
        when(orgA.getId()).thenReturn(uuid);
        when(orgA.getName()).thenReturn(name);

        when(organizationsDao.get(eq(uuid.toString()))).thenReturn(orgA);

        return orgA;
    }

    @Test
    public void create_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "name");
        when(dao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);

        final Response response = resources
                .target("/organizations/" + org.getId().toString() + "/people")
                .request()
                .post(Entity.json(new PersonDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final PersonDTO dto = response.readEntity(PersonDTO.class);
        assertThat(response.getLocation().getPath()).isEqualTo("/organizations/" + org.getId() + "/people/" + dto.id);
        assertThat(dto.id).isEqualTo(person.getId().toString());
        assertThat(dto.name).isEqualTo("name");

        verify(dao).create(any(Organization.class), any(PersonProperties.class));
        verify(organizationsDao).get(eq(org.getId().toString()));
    }

}