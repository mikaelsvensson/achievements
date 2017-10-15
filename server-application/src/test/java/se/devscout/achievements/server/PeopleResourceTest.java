package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.PeopleResource;
import se.devscout.achievements.server.auth.User;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PeopleResourceTest {

    private static final int ZERO = 0;
    private final PeopleDao dao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
//            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addProvider(AchievementsApplication.createAuthFeature(mock(SessionFactory.class), credentialsDao))
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthValueFactoryProvider.Binder<>(User.class))

            .addResource(new PeopleResource(dao, organizationsDao))
            .build();

    @Before
    public void setUp() throws Exception {
        final Credentials credentials = new Credentials("username", new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray()));
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("user"))).thenReturn(credentials);
    }

    @Test
    public void get_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "Alice");

        final Response response = resources
                .target("/organizations/" + org.getId() + "/people/" + person.getId())
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final PersonDTO dto = response.readEntity(PersonDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.id).isNotEqualTo(ZERO);

        verify(dao).read(eq(person.getId()));
    }

    @Test
    public void getByOrganization_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "Alice");
        when(dao.getByParent(eq(org))).thenReturn(Collections.singletonList(person));

        final Response response = resources
                .target("/organizations/" + org.getId() + "/people")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final List<PersonDTO> dto = response.readEntity(new GenericType<List<PersonDTO>>() {
        });
        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).id).isNotNull();
        assertThat(dto.get(0).id).isNotEqualTo(ZERO);

        verify(dao).getByParent(eq(org));
    }

    @Test
    public void getByOrganization_missing_expectNotFound() throws Exception {
        final UUID badId = UUID.randomUUID();
        when(organizationsDao.read(eq(badId))).thenThrow(new NotFoundException());

        final Response response = resources
                .target("/organizations/" + badId.toString() + "/people")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao, never()).getByParent(any(Organization.class));
    }

    @Test
    public void get_notFound() throws Exception {
        when(dao.read(eq(123))).thenThrow(new NotFoundException());
        final Response response = resources
                .target("/organizations/" + UUID.randomUUID().toString() + "/people/123")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao).read(eq(123));
    }

    @Test
    public void delete_notFound() throws Exception {
        doThrow(new NotFoundException()).when(dao).read(eq(-1));

        final Response response = resources
                .target("/organizations/ORG_ID/people/PERSON_ID")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao, never()).delete(anyInt());
    }

    @Test
    public void delete_happyPath() throws Exception {
        final Organization org = mockOrganization("Org");
        final Person person = mockPerson(org, "name");

        final Response response = resources
                .target("/organizations/" + org.getId() + "/people/" + person.getId())
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(dao).delete(eq(person.getId()));
    }

    @Test
    public void delete_wrongOrganization_expectNotFound() throws Exception {

        final Organization orgA = mockOrganization("ORG_A");
        final Organization orgB = mockOrganization("ORG_B");
        final Person person = mockPerson(orgA, "name");

        final Response response = resources
                .target("/organizations/" + orgB.getId() + "/people/" + person.getId())
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(organizationsDao).read(eq(orgB.getId()));
        verify(dao, never()).delete(anyInt());
    }

    @Test
    public void get_wrongOrganization_expectNotFound() throws Exception {

        final Organization orgA = mockOrganization("ORG_A");
        final Organization orgB = mockOrganization("ORG_B");
        final Person person = mockPerson(orgA, "name");

        final Response response = resources
                .target("/organizations/" + orgB.getId() + "/people/" + person.getId())
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(organizationsDao).read(eq(orgB.getId()));
        verify(dao).read(eq(person.getId()));
    }

    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException {
        final Integer id = getRandomNonZeroValue();

        final Person person = mock(Person.class);
        when(person.getId()).thenReturn(id);
        when(person.getOrganization()).thenReturn(org);
        when(person.getName()).thenReturn(name);

        when(dao.read(eq(id))).thenReturn(person);

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

    @Test
    public void create_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "name");
        when(dao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);

        final Response response = resources
                .target("/organizations/" + org.getId().toString() + "/people")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new PersonDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final PersonDTO dto = response.readEntity(PersonDTO.class);

        assertThat(response.getLocation().getPath()).isEqualTo("/organizations/" + org.getId() + "/people/" + person.getId());
        assertThat(dto.name).isEqualTo("name");

        verify(dao).create(any(Organization.class), any(PersonProperties.class));
        verify(organizationsDao).read(eq(org.getId()));
    }

}