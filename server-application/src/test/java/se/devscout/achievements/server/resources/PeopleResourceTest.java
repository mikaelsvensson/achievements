package se.devscout.achievements.server.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.OrganizationAchievementSummaryDTO;
import se.devscout.achievements.server.api.PersonBaseDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static se.devscout.achievements.server.MockUtil.*;

public class PeopleResourceTest {

    private static final int ZERO = 0;
    private final PeopleDao dao = mock(PeopleDao.class);

    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);

    private final AchievementsDao achievementsDao = mock(AchievementsDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new PeopleResource(dao, organizationsDao, achievementsDao))
            .build();

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
    }


    @Test
    public void get_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "Alice");

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final PersonDTO dto = response.readEntity(PersonDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.id).isNotEqualTo(ZERO);

        verify(dao).read(eq(person.getId()));
    }

    private Organization mockOrganization(String name) throws ObjectNotFoundException {
        final Organization org = MockUtil.mockOrganization(name);
        when(organizationsDao.read(eq(org.getId()))).thenReturn(org);
        return org;
    }

    @Test
    public void getByOrganization_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "Alice");
        when(dao.getByParent(eq(org))).thenReturn(Collections.singletonList(person));

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
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
                .target("/organizations/" + UuidString.toString(badId) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao, never()).getByParent(any(Organization.class));
    }

    @Test
    public void get_notFound() throws Exception {
        when(dao.read(eq(123))).thenThrow(new NotFoundException());
        final Response response = resources
                .target("/organizations/" + UuidString.toString(UUID.randomUUID()) + "/people/123")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao).read(eq(123));
    }

    @Test
    public void get_byCustomId_notFound() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "Alice", "alice");
        when(dao.read(eq(org), eq("alice"))).thenReturn(person);
        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/c:alice")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final PersonDTO dto = response.readEntity(PersonDTO.class);
        assertThat(dto.name).isEqualTo("Alice");
        assertThat(dto.custom_identifier).isEqualTo("alice");

        verify(dao, never()).read(anyInt());
        verify(dao).read(eq(org), eq("alice"));
    }

    @Test
    public void delete_notFound() throws Exception {
        doThrow(new NotFoundException()).when(dao).read(eq(-1));

        final Response response = resources
                .target("/organizations/ORG_ID/people/PERSON_ID")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao, never()).delete(anyInt());
    }

    @Test
    public void delete_happyPath() throws Exception {
        final Organization org = mockOrganization("Org");
        final Person person = mockPerson(org, "name");

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(dao).delete(eq(person.getId()));
    }

    @Test
    public void delete_unauthorizedUser_expectForbidden() throws Exception {
        final Organization org = mockOrganization("Org");
        final Person person = mockPerson(org, "name");

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(dao, never()).delete(eq(person.getId()));
    }

    @Test
    public void delete_wrongOrganization_expectNotFound() throws Exception {

        final Organization orgA = mockOrganization("ORG_A");
        final Organization orgB = mockOrganization("ORG_B");
        final Person person = mockPerson(orgA, "name");

        final Response response = resources
                .target("/organizations/" + UuidString.toString(orgB.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
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
                .target("/organizations/" + UuidString.toString(orgB.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(organizationsDao).read(eq(orgB.getId()));
        verify(dao).read(eq(person.getId()));
    }

    @Test
    public void create_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "name");
        when(dao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.json(new PersonDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final PersonDTO dto = response.readEntity(PersonDTO.class);

        assertThat(response.getLocation().getPath()).isEqualTo("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId());
        assertThat(dto.name).isEqualTo("name");

        verify(dao).create(any(Organization.class), any(PersonProperties.class));
        verify(organizationsDao).read(eq(org.getId()));
    }

    @Test
    public void create_unauthorizedUser_expectForbidden() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "name");
        when(dao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .post(Entity.json(new PersonDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
    }

    @Test
    public void batchUpdate_json_happyPath() throws Exception {
        final Organization org = mockOrganization("org");

        final Person alice = mockPerson(org, "Alice");
        when(dao.read(eq(org), eq("aaa"))).thenReturn(alice);
        final ArgumentCaptor<PersonProperties> updateCaptor = ArgumentCaptor.forClass(PersonProperties.class);
        when(dao.update(eq(alice.getId()), updateCaptor.capture())).thenReturn(alice);

        final Person bob = mockPerson(org, "Bob");
        when(dao.read(eq(org), eq("bbb"))).thenReturn(bob);

        final Person carol = mockPerson(org, "Carol");
        when(dao.read(eq(org), eq("ccc"))).thenThrow(new ObjectNotFoundException());
        final ArgumentCaptor<PersonProperties> createCaptor = ArgumentCaptor.forClass(PersonProperties.class);
        when(dao.create(any(Organization.class), createCaptor.capture())).thenReturn(carol);

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.json(Arrays.asList(
                        new PersonDTO(-1, "Alicia", "alice@example.com", "aaa", null),
                        new PersonDTO(-1, "Carol", "carol@example.com", "ccc", null)
                )));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final List<PersonBaseDTO> dto = response.readEntity(new GenericType<List<PersonBaseDTO>>() {
        });
        assertThat(dto).hasSize(2);
        assertThat(dto.get(0).id).isNotNull();
        assertThat(dto.get(0).name).isNotNull();
        assertThat(dto.get(1).id).isNotNull();
        assertThat(dto.get(1).name).isNotNull();

        verify(dao).read(eq(org), eq("ccc"));
        verify(dao).create(eq(org), any(PersonProperties.class));
        assertThat(createCaptor.getValue().getCustomIdentifier()).isEqualTo("ccc");
        assertThat(createCaptor.getValue().getName()).isEqualTo("Carol");
        assertThat(createCaptor.getValue().getEmail()).isEqualTo("carol@example.com");

        verify(dao).read(eq(org), eq("aaa"));
        verify(dao).update(eq(alice.getId()), any(PersonProperties.class));
        assertThat(updateCaptor.getValue().getCustomIdentifier()).isEqualTo("aaa");
        assertThat(updateCaptor.getValue().getName()).isEqualTo("Alicia");
        assertThat(updateCaptor.getValue().getEmail()).isEqualTo("alice@example.com");

        verify(organizationsDao).read(eq(org.getId()));
    }

    @Test
    public void batchUpdateJson_unauthorizedUser_expectForbidden() throws Exception {
        final Organization org = mockOrganization("org");

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .put(Entity.json(Arrays.asList(
                        new PersonDTO(-1, "Alicia", "alice@example.com", "aaa", null),
                        new PersonDTO(-1, "Carol", "carol@example.com", "ccc", null)
                )));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(dao, never()).update(anyInt(), any(PersonProperties.class));
    }

    @Test
    public void batchUpdateCsv_unauthorizedUser_expectForbidden() throws Exception {
        final Organization org = mockOrganization("org");

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .put(Entity.entity("" +
                                "name,email,custom_identifier\n" +
                                "Alicia,alice@example.com,aaa\n" +
                                "Carol,carol@example.com,ccc\n",
                        "text/csv"
                ));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(dao, never()).update(anyInt(), any(PersonProperties.class));
    }

    @Test
    public void batchUpdate_csv_happyPath() throws Exception {
        final Organization org = mockOrganization("org");

        final Person alice = mockPerson(org, "Alice");
        when(dao.read(eq(org), eq("aaa"))).thenReturn(alice);
        final ArgumentCaptor<PersonProperties> updateCaptor = ArgumentCaptor.forClass(PersonProperties.class);
        when(dao.update(eq(alice.getId()), updateCaptor.capture())).thenReturn(alice);

        final Person bob = mockPerson(org, "Bob");
        when(dao.read(eq(org), eq("bbb"))).thenReturn(bob);

        final Person carol = mockPerson(org, "Carol");
        when(dao.read(eq(org), eq("ccc"))).thenThrow(new ObjectNotFoundException());
        final ArgumentCaptor<PersonProperties> createCaptor = ArgumentCaptor.forClass(PersonProperties.class);
        when(dao.create(any(Organization.class), createCaptor.capture())).thenReturn(carol);

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.entity("" +
                                "name,email,custom_identifier\n" +
                                "Alicia,alice@example.com,aaa\n" +
                                "Carol,carol@example.com,ccc\n",
                        "text/csv"
                ));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final List<PersonBaseDTO> dto = response.readEntity(new GenericType<List<PersonBaseDTO>>() {
        });
        assertThat(dto).hasSize(2);
        assertThat(dto.get(0).id).isNotNull();
        assertThat(dto.get(0).name).isNotNull();
        assertThat(dto.get(1).id).isNotNull();
        assertThat(dto.get(1).name).isNotNull();

        verify(dao).read(eq(org), eq("ccc"));
        verify(dao).create(eq(org), any(PersonProperties.class));
        assertThat(createCaptor.getValue().getCustomIdentifier()).isEqualTo("ccc");
        assertThat(createCaptor.getValue().getName()).isEqualTo("Carol");
        assertThat(createCaptor.getValue().getEmail()).isEqualTo("carol@example.com");

        verify(dao).read(eq(org), eq("aaa"));
        verify(dao).update(eq(alice.getId()), any(PersonProperties.class));
        assertThat(updateCaptor.getValue().getCustomIdentifier()).isEqualTo("aaa");
        assertThat(updateCaptor.getValue().getName()).isEqualTo("Alicia");
        assertThat(updateCaptor.getValue().getEmail()).isEqualTo("alice@example.com");

        verify(organizationsDao).read(eq(org.getId()));
    }

    @Test
    public void achievementSummary_twoAchievementTwoSteps_successful() throws Exception {
        final Organization org = mockOrganization("Alice's Organization");
        final Person person1 = mockPerson(org, "Alice");
        final Person person2 = mockPerson(org, "Bob");
        final Person person3 = mockPerson(org, "Carol");
        final AchievementStepProgress a1p1 = mockProgress(true, person1);
        final AchievementStepProgress a1p2 = mockProgress(true, person2);
        final AchievementStepProgress a1p3 = mockProgress(true, person1);
        final AchievementStepProgress a1p4 = mockProgress(true, person2);
        final AchievementStep a1s1 = mockStep(a1p1, a1p2);
        final AchievementStep a1s2 = mockStep(a1p3, a1p4);
        final Achievement a1 = mockAchievement("Climb mountain", a1s1, a1s2);

        final AchievementStepProgress a2p1 = mockProgress(false, person2);
        final AchievementStepProgress a2p2 = mockProgress(true, person1);
        final AchievementStepProgress a2p3 = mockProgress(false, person2);
        final AchievementStepProgress a2p4 = mockProgress(false, person1);
        final AchievementStep s2 = mockStep(a2p1, a2p2);
        final AchievementStep s3 = mockStep(a2p3, a2p4);
        final Achievement a2 = mockAchievement("Cook egg", s2, s3);

        when(achievementsDao.findWithProgressForPerson(eq(person1)))
                .thenReturn(Arrays.asList(a1, a2));

        final OrganizationAchievementSummaryDTO dto = resources.client()
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person1.getId() + "/achievement-summary")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get(OrganizationAchievementSummaryDTO.class);

        assertThat(dto.achievements).hasSize(2);

        assertThat(dto.achievements.get(0).achievement.name).isEqualTo("Climb mountain");
        assertThat(dto.achievements.get(0).progress_summary.people_completed).isEqualTo(1);
        assertThat(dto.achievements.get(0).progress_summary.people_started).isEqualTo(0);
        assertThat(dto.achievements.get(0).progress_detailed).hasSize(1);
        assertThat(dto.achievements.get(0).progress_detailed.get(0).person.name).isEqualTo("Alice");
        assertThat(dto.achievements.get(0).progress_detailed.get(0).percent).isEqualTo(100);

        assertThat(dto.achievements.get(1).achievement.name).isEqualTo("Cook egg");
        assertThat(dto.achievements.get(1).progress_summary.people_completed).isEqualTo(0);
        assertThat(dto.achievements.get(1).progress_summary.people_started).isEqualTo(1);
        assertThat(dto.achievements.get(1).progress_detailed).hasSize(1);
        assertThat(dto.achievements.get(1).progress_detailed.get(0).person.name).isEqualTo("Alice");
        assertThat(dto.achievements.get(1).progress_detailed.get(0).percent).isEqualTo(50);
    }

    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException, IOException {
        return mockPerson(org, name, null);
    }

    private Person mockPerson(Organization org, String name, String customId) throws ObjectNotFoundException, IOException {
        final Person person1 = MockUtil.mockPerson(org, name, customId, Roles.READER);
        when(dao.read(eq(person1.getId()))).thenReturn(person1);
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
        final Credentials credentials = new Credentials("username", passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData());
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(name))).thenReturn(credentials);
        return person1;
    }
}