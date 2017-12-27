package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.api.PersonProfileDTO;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.MyResource;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class MyResourceTest {

    private static final int ZERO = 0;
    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final AchievementsDao achievementsDao = mock(AchievementsDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao, peopleDao)
            .addResource(new MyResource(peopleDao, achievementsDao))
            .build();

    @Before
    public void setUp() throws Exception {
        final Credentials credentials = new Credentials("username", new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray()));
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("user"))).thenReturn(credentials);
    }

    @Test
    public void getMyPeople_happyPath() throws Exception {
        final Organization org1 = mockOrganization("Monsters Inc.");
        final Person person1 = mockPerson(org1, "Alice");
        final Organization org2 = mockOrganization("Cyberdyne Systems");
        final Person person2a = mockPerson(org2, "Bob");
        final Person person2b = mockPerson(org2, "Carol");
        final Credentials credentials = new Credentials("bob", new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray()));
        credentials.setPerson(person2a);
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("bob"))).thenReturn(credentials);

        when(peopleDao.getByParent(eq(org1))).thenReturn(Lists.newArrayList(person1));
        when(peopleDao.getByParent(eq(org2))).thenReturn(Lists.newArrayList(person2a, person2b));
        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org1, org2));

        final Response response = resources
                .target("/my/people/")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("bob:pw".getBytes(Charsets.UTF_8)))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final List<PersonDTO> dto = response.readEntity(new GenericType<List<PersonDTO>>() {
        });
        assertThat(dto).hasSize(2);
        assertThat(dto.get(0).id).isNotNull();
        assertThat(dto.get(0).id).isNotEqualTo(ZERO);
        assertThat(dto.get(0).name).isEqualTo("Bob");
        assertThat(dto.get(1).id).isNotNull();
        assertThat(dto.get(1).id).isNotEqualTo(ZERO);
        assertThat(dto.get(1).name).isEqualTo("Carol");

        verify(peopleDao, never()).getByParent(eq(org1));
        verify(peopleDao).getByParent(eq(org2));
    }

    @Test
    public void getMyProfile_happyPath() throws Exception {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        final Credentials credentials = new Credentials("bob", new PasswordValidator(SecretGenerator.PDKDF2, "pw".toCharArray()));
        credentials.setPerson(person);
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("bob"))).thenReturn(credentials);

        when(peopleDao.getByParent(eq(org))).thenReturn(Lists.newArrayList(person));
        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("/my/profile/")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("bob:pw".getBytes(Charsets.UTF_8)))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final PersonProfileDTO dto = response.readEntity(PersonProfileDTO.class);
        assertThat(dto.person.id).isNotNull();
        assertThat(dto.person.name).isEqualTo("Bob");
        assertThat(dto.person.email).isEqualTo("user@example.com");
        assertThat(dto.organization.name).isEqualTo("Cyberdyne Systems");
    }


    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException {
        final Integer uuid = getRandomNonZeroValue();

        final Person person = mock(Person.class);
        when(person.getId()).thenReturn(uuid);
        when(person.getOrganization()).thenReturn(org);
        when(person.getName()).thenReturn(name);
        when(person.getEmail()).thenReturn("user@example.com");

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