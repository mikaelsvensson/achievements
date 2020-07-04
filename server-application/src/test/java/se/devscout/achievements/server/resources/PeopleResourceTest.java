package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.commons.text.RandomStringGenerator;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import se.devscout.achievements.server.I18n;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static se.devscout.achievements.server.MockUtil.*;

public class PeopleResourceTest {

    private static final int ZERO = 0;
    private static final int UUID_STRING_LENGTH = UUID.randomUUID().toString().length();
    private final PeopleDao dao = mock(PeopleDao.class);

    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);

    private final AchievementsDao achievementsDao = mock(AchievementsDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    private final GroupsDao groupsDao = mock(GroupsDao.class);

    private final EmailSender emailSender = mock(EmailSender.class);

    private final I18n i18n = mock(I18n.class);

    private final GroupMembershipsDao membershipsDao = mock(GroupMembershipsDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addProvider(MultiPartFeature.class)
            .addResource(new PeopleResource(
                    dao,
                    organizationsDao,
                    achievementsDao,
                    new ObjectMapper(),
                    groupsDao,
                    membershipsDao,
                    URI.create("http://gui/"),
                    emailSender,
                    i18n))
            .build();

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
        when(i18n.get(anyString())).thenReturn("default i18n string");
    }


    @Test
    public void get_happyPath() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "Alice");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var dto = response.readEntity(PersonDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.id).isNotEqualTo(ZERO);

        verify(dao).read(eq(person.getId()));
    }

    @Test
    public void welcomeMail_happyPath() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "Alice");
        when(person.getEmail()).thenReturn("alice@gmail.com");
        when(person.getCredentials()).thenReturn(Collections.emptySet());

        when(i18n.get(anyString())).thenReturn("the subject");
        final var bodyCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(emailSender).send(anyString(), anyString(), anyString(), bodyCaptor.capture());

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId() + "/mails/welcome")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(dao).read(eq(person.getId()));
        verify(emailSender).send(
                anyString(),
                anyString(),
                eq("the subject"),
                anyString());
        assertThat(bodyCaptor.getValue()).contains("Du kan nu anv\u00e4nda Mina m\u00e4rken");
        assertThat(bodyCaptor.getValue()).contains("Logga in med Google");
        assertThat(bodyCaptor.getValue()).doesNotContain("Logga in med Microsoft");
        assertThat(bodyCaptor.getValue()).doesNotContain("Logga in med e-post");
    }

    @Test
    public void welcomeMail_noEmail() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "Alice");
        when(person.getEmail()).thenReturn(null);

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId() + "/mails/welcome")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(emailSender, never()).send(
                anyString(),
                anyString(),
                anyString(),
                anyString());
    }

    @Test
    public void welcomeMail_sendException() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "Alice");
        when(person.getEmail()).thenReturn("<script>alert('Trudy was here')</script>");
        doThrow(new EmailSenderException("Error")).when(emailSender).send(
                anyString(),
                anyString(),
                anyString(),
                anyString());

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId() + "/mails/welcome")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);

        verify(emailSender, never()).send(
                anyString(),
                anyString(),
                anyString(),
                anyString());
    }

    @Test
    public void welcomeMail_emptyEmail() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "Alice");
        when(person.getEmail()).thenReturn("   ");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId() + "/mails/welcome")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(emailSender, never()).send(
                anyString(),
                anyString(),
                anyString(),
                anyString());
    }

    @Test
    public void get_authReader_expectUnauthorized() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "Alice");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(dao, never()).getByParent(any(Organization.class));
    }

    private Organization mockOrganization(String name) throws ObjectNotFoundException {
        final var org = MockUtil.mockOrganization(name);
        when(organizationsDao.read(eq(org.getId()))).thenReturn(org);
        return org;
    }

    @Test
    public void getByOrganization_authReader_happyPath() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "Alice");
        when(dao.getByParent(eq(org))).thenReturn(Collections.singletonList(person));

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        // A list of PersonBaseDTO should be returned but the point is that we don't accidentally want to return a PersonDTO with custom_identifier value.
        final var dto = response.readEntity(new GenericType<List<PersonDTO>>() {
        });
        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).id).isNotNull();
        assertThat(dto.get(0).id).isNotEqualTo(ZERO);
        assertThat(dto.get(0).custom_identifier).isNullOrEmpty();

        verify(dao).getByParent(eq(org));
    }

    @Test
    public void getByOrganization_missing_expectNotFound() throws Exception {
        final var badId = UUID.randomUUID();
        when(organizationsDao.read(eq(badId))).thenThrow(new NotFoundException());

        final var response = resources
                .target("/organizations/" + UuidString.toString(badId) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao, never()).getByParent(any(Organization.class));
    }

    @Test
    public void getByOrganization_filterByGroup_happyPath() throws Exception {
        final var org = mockOrganization("org");

        final var group1 = mockGroup(org, "Developers");
        final var group2 = mockGroup(org, "Marketing");

        final var person1 = mockPerson(org, "Alice");
        when(person1.getMemberships()).thenReturn(Collections.singleton(new GroupMembership(group1, person1, GroupRole.MEMBER)));

        final var person2 = mockPerson(org, "Bob");
        when(person2.getMemberships()).thenReturn(Collections.singleton(new GroupMembership(group1, person2, GroupRole.MANAGER)));

        final var person3 = mockPerson(org, "Carol");
        when(person3.getMemberships()).thenReturn(Collections.singleton(new GroupMembership(group2, person3, GroupRole.MEMBER)));

        when(dao.getByParent(eq(org))).thenReturn(Arrays.asList(person1, person2, person3));

        final var response1 = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .queryParam("group", group1.getId().toString())
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var dto1 = response1.readEntity(new GenericType<List<PersonBaseDTO>>() {
        });
        assertThat(dto1.stream().map(personBaseDTO -> personBaseDTO.name).collect(Collectors.toList())).containsExactlyInAnyOrder("Alice", "Bob");

        final var response2 = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .queryParam("filter", "a")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response2.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var dto2 = response2.readEntity(new GenericType<List<PersonBaseDTO>>() {
        });
        assertThat(dto2.stream().map(personBaseDTO -> personBaseDTO.name).collect(Collectors.toList())).containsExactlyInAnyOrder("Alice", "Carol");

        final var response3 = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response3.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var dto3 = response3.readEntity(new GenericType<List<PersonBaseDTO>>() {
        });
        assertThat(dto3.stream().map(personBaseDTO -> personBaseDTO.name).collect(Collectors.toList())).containsExactlyInAnyOrder("Alice", "Bob", "Carol");

    }

    @Test
    public void get_notFound() throws Exception {
        when(dao.read(eq(123))).thenThrow(new NotFoundException());
        final var response = resources
                .target("/organizations/" + UuidString.toString(UUID.randomUUID()) + "/people/123")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao).read(eq(123));
    }

    @Test
    public void get_byCustomId_notFound() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "Alice", "alice");
        when(dao.read(eq(org), eq("alice"))).thenReturn(person);
        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/c:alice")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var dto = response.readEntity(PersonDTO.class);
        assertThat(dto.name).isEqualTo("Alice");
        assertThat(dto.custom_identifier).isEqualTo("alice");

        verify(dao, never()).read(anyInt());
        verify(dao).read(eq(org), eq("alice"));
    }

    @Test
    public void delete_notFound() throws Exception {
        doThrow(new NotFoundException()).when(dao).read(eq(-1));

        final var response = resources
                .target("/organizations/ORG_ID/people/PERSON_ID")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(dao, never()).delete(anyInt());
    }

    @Test
    public void delete_happyPath() throws Exception {
        final var org = mockOrganization("Org");
        final var person = mockPerson(org, "name");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(dao).delete(eq(person.getId()));
    }

    @Test
    public void delete_unauthorizedUser_expectForbidden() throws Exception {
        final var org = mockOrganization("Org");
        final var person = mockPerson(org, "name");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(dao, never()).delete(eq(person.getId()));
    }

    @Test
    public void delete_wrongOrganization_expectNotFound() throws Exception {

        final var orgA = mockOrganization("ORG_A");
        final var orgB = mockOrganization("ORG_B");
        final var person = mockPerson(orgA, "name");

        final var response = resources
                .target("/organizations/" + UuidString.toString(orgB.getId()) + "/people/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(organizationsDao).read(eq(orgB.getId()));
        verify(dao, never()).delete(anyInt());
    }

    @Test
    public void delete_self_expectBadRequest() throws Exception {
        final var org = mockOrganization("org");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + credentialsDao.get(CredentialsType.PASSWORD, USERNAME_EDITOR).getPerson().getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(dao, never()).delete(anyInt());
    }

    @Test
    public void get_wrongOrganization_expectNotFound() throws Exception {

        final var orgA = mockOrganization("ORG_A");
        final var orgB = mockOrganization("ORG_B");
        final var person = mockPerson(orgA, "name");

        final var response = resources
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
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "name");
        when(dao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.json(new PersonDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final var dto = response.readEntity(PersonDTO.class);

        assertThat(response.getLocation().getPath()).isEqualTo("/organizations/" + UuidString.toString(org.getId()) + "/people/" + person.getId());
        assertThat(dto.name).isEqualTo("name");

        verify(dao).create(any(Organization.class), any(PersonProperties.class));
        verify(organizationsDao).read(eq(org.getId()));
    }

    @Test
    public void create_unauthorizedUser_expectForbidden() throws Exception {
        final var org = mockOrganization("org");
        final var person = mockPerson(org, "name");
        when(dao.create(any(Organization.class), any(PersonProperties.class))).thenReturn(person);

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .post(Entity.json(new PersonDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
    }

    @Test
    public void create_privilegeEscalation_expectBadRequest() throws Exception {
        final var org = mockOrganization("org");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.json(new PersonDTO(null, "Alice", "admin")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
    }

    @Test
    public void update_changeName_happyPath() throws Exception {
        final var org = mockOrganization("org");
        final var expectedPerson = mockPerson(org, "Alicia");
        when(dao.read(eq(expectedPerson.getId()))).thenReturn(expectedPerson);
        when(dao.update(eq(expectedPerson.getId()), any(PersonProperties.class))).thenReturn(expectedPerson);

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + expectedPerson.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.json(new PersonDTO(null, "Alicia")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        final var dto = response.readEntity(PersonDTO.class);

        assertThat(dto.name).isEqualTo("Alicia");

        verify(dao).update(eq(expectedPerson.getId()), any(PersonProperties.class));
    }

    @Test
    public void update_privilegeEscalation_expectBadRequest() throws Exception {
        final var org = mockOrganization("org");
        final var expectedPerson = mockPerson(org, "Alicia");
        when(dao.read(eq(expectedPerson.getId()))).thenReturn(expectedPerson);
        when(dao.update(eq(expectedPerson.getId()), any(PersonProperties.class))).thenReturn(expectedPerson);

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + expectedPerson.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.json(new PersonDTO(null, "Alicia", "admin")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(dao, never()).update(eq(expectedPerson.getId()), any(PersonProperties.class));
    }

    @Test
    public void update_self_expectBadRequest() throws Exception {
        final var org = mockOrganization("org");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people/" + credentialsDao.get(CredentialsType.PASSWORD, USERNAME_EDITOR).getPerson().getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.json(new PersonDTO(null, "Alice")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(dao, never()).update(anyInt(), any(PersonProperties.class));
    }

    @Test
    public void batchUpdate_json_happyPath() throws Exception {
        final var org = mockOrganization("org");

        final var alice = mockPerson(org, "Alice");
        when(dao.read(eq(org), eq("aaa"))).thenReturn(alice);
        final var updateCaptor = ArgumentCaptor.forClass(PersonProperties.class);
        when(dao.update(eq(alice.getId()), updateCaptor.capture())).thenReturn(alice);

        final var bob = mockPerson(org, "Bob");
        when(dao.read(eq(org), eq("bbb"))).thenReturn(bob);

        final var carol = mockPerson(org, "Carol");
        when(dao.read(eq(org), eq("ccc"))).thenThrow(new ObjectNotFoundException());
        final var createCaptor = ArgumentCaptor.forClass(PersonProperties.class);
        when(dao.create(any(Organization.class), createCaptor.capture())).thenReturn(carol);

        final var group = mockGroup(org, "Developers");
        when(groupsDao.create(eq(org), any())).thenReturn(group);
        when(groupsDao.read(eq(org), eq("Developers")))
                .thenThrow(new ObjectNotFoundException())
                .thenReturn(group);

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.json(Arrays.asList(
                        new PersonDTO(-1, "Alicia", "alice@example.com", "aaa", null, null, null, Lists.newArrayList(new GroupBaseDTO(null, "Developers"))),
                        new PersonDTO(-1, "Carol", "carol@example.com", "ccc", null, null, Collections.singletonList(new PersonAttributeDTO("title", "Boss")), Lists.newArrayList(new GroupBaseDTO(null, "Developers")))
                )));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var dto = response.readEntity(new GenericType<List<PersonBaseDTO>>() {
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
        assertThat(createCaptor.getValue().getAttributes()).contains(new PersonAttribute("title", "Boss"));

        verify(dao).read(eq(org), eq("aaa"));
        verify(dao).update(eq(alice.getId()), any(PersonProperties.class));
        assertThat(updateCaptor.getValue().getCustomIdentifier()).isEqualTo("aaa");
        assertThat(updateCaptor.getValue().getName()).isEqualTo("Alicia");
        assertThat(updateCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(updateCaptor.getValue().getAttributes()).isNull();

        verify(organizationsDao).read(eq(org.getId()));
        verify(groupsDao, times(2)).read(eq(org), eq("Developers"));
        verify(groupsDao, times(1)).create(eq(org), any());
        verify(membershipsDao).add(eq(alice), eq(group), any());
        verify(membershipsDao).add(eq(carol), eq(group), any());
    }

    @Test
    public void batchUpdateJson_unauthorizedUser_expectForbidden() throws Exception {
        final var org = mockOrganization("org");

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .put(Entity.json(Arrays.asList(
                        new PersonDTO(-1, "Alicia", "alice@example.com", "aaa", null, null, null, null),
                        new PersonDTO(-1, "Carol", "carol@example.com", "ccc", null, null, null, null)
                )));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(dao, never()).update(anyInt(), any(PersonProperties.class));
    }

    @Test
    public void batchUpdateCsv_unauthorizedUser_expectForbidden() throws Exception {
        final var org = mockOrganization("org");

        final var response = resources
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
        final var org = mockOrganization("org");

        final var alice = mockPerson(org, "Alice");
        when(dao.read(eq(org), eq("aaa"))).thenReturn(alice);
        final var updateCaptor = ArgumentCaptor.forClass(PersonProperties.class);
        when(dao.update(eq(alice.getId()), updateCaptor.capture())).thenReturn(alice);

        final var bob = mockPerson(org, "Bob");
        when(dao.read(eq(org), eq("bbb"))).thenReturn(bob);

        final var carol = mockPerson(org, "Carol");
        when(dao.read(eq(org), eq("ccc"))).thenThrow(new ObjectNotFoundException());
        final var createCaptor = ArgumentCaptor.forClass(PersonProperties.class);
        when(dao.create(any(Organization.class), createCaptor.capture())).thenReturn(carol);

        final var groupDev = mockGroup(org, "Developers");
        final var groupMgr = mockGroup(org, "Managers");
        when(groupsDao.create(eq(org), any()))
                .thenReturn(groupDev)
                .thenReturn(groupMgr);
        when(groupsDao.read(eq(org), eq("Developers")))
                .thenThrow(new ObjectNotFoundException())
                .thenReturn(groupDev);
        when(groupsDao.read(eq(org), eq("Managers")))
                .thenThrow(new ObjectNotFoundException())
                .thenReturn(groupMgr);

        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.entity("" +
                                "name,attr.tag,email,custom_identifier,groups\n" +
                                "Alicia,boss,alice@example.com,aaa,Developers\n" +
                                "Carol,minion,carol@example.com,ccc,\"Developers , Managers \"\n",
                        "text/csv"
                ));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final var dto = response.readEntity(new GenericType<List<PersonBaseDTO>>() {
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
        assertThat(createCaptor.getValue().getAttributes()).contains(new PersonAttribute("tag", "minion"));

        verify(dao).read(eq(org), eq("aaa"));
        verify(dao).update(eq(alice.getId()), any(PersonProperties.class));
        assertThat(updateCaptor.getValue().getCustomIdentifier()).isEqualTo("aaa");
        assertThat(updateCaptor.getValue().getName()).isEqualTo("Alicia");
        assertThat(updateCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(updateCaptor.getValue().getAttributes()).contains(new PersonAttribute("tag", "boss"));

        verify(organizationsDao).read(eq(org.getId()));
        verify(groupsDao, times(2)).read(eq(org), eq("Developers"));
        verify(groupsDao, times(1)).read(eq(org), eq("Managers"));
        verify(groupsDao, times(2)).create(eq(org), any());
        verify(membershipsDao).add(eq(alice), eq(groupDev), any());
        verify(membershipsDao).add(eq(carol), eq(groupDev), any());
        verify(membershipsDao).add(eq(carol), eq(groupMgr), any());
    }

    @Test
    public void batchUpdate_repetFileAndFileId_happyPath() throws Exception {
        //
        // MOCK STUFF
        //
        final var org = mockOrganization("Acme Inc.");

        when(dao.read(eq(org), anyString())).thenThrow(new ObjectNotFoundException());
        final var carol = mockPerson(org, "Person");
        when(dao.create(any(Organization.class), any())).thenReturn(carol);

        when(groupsDao.read(eq(org), anyString())).thenThrow(new ObjectNotFoundException());
        final var groupDev = mockGroup(org, "Group");
        when(groupsDao.create(eq(org), any())).thenReturn(groupDev);

        //
        // REQUEST 1: Upload an XML file with data from Repet.
        //

        final var fileDataBodyPart = new FileDataBodyPart(
                "importFile",
                new File(getClass().getClassLoader().getResource("batchupsert-repet-narvarolista.xml").getFile()));
        final var multiPartReq1 = (FormDataMultiPart) new FormDataMultiPart()
                .bodyPart(fileDataBodyPart);
        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MultiPartFeature.class)
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.entity(multiPartReq1, multiPartReq1.getMediaType()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        // These assertions are essentially just to validate that the returned JSON is in the expected format. The data itself is incorrect since it is all mocked.
        final var dto = response.readEntity(UpsertResultDTO.class);
        assertThat(dto.uploadId).hasSize(UUID_STRING_LENGTH);
        assertThat(dto.people).hasSize(36);
        assertThat(dto.people.stream().allMatch(p -> p.isNew)).isTrue();
        assertThat(dto.people.stream().allMatch(p -> p.person.name.length() > 0)).isTrue();

        //
        // REQUEST 2: Import the same data once more, but this time by referencing the previously uploaded file instead of uploading it again.
        //

        final var multiPartReq2 = new FormDataMultiPart()
                .field("importUploadedFileId", dto.uploadId);
        final var response2 = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MultiPartFeature.class)
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.entity(multiPartReq2, multiPartReq2.getMediaType()));

        assertThat(response2.getStatus()).isEqualTo(HttpStatus.OK_200);

        // These assertions are essentially just to validate that the returned JSON is in the expected format.
        final var dto2 = response2.readEntity(UpsertResultDTO.class);
        assertThat(dto2.uploadId).isEqualTo(dto.uploadId);
        // Asserting the number of people returned is important as it validates that we reused the file previously uploaded by just referencing it this time.
        assertThat(dto2.people).hasSize(36);
        assertThat(dto2.people.stream().allMatch(p -> p.person.name.length() > 0)).isTrue();
        // Assert that the isNew flags are present, even though their values are all incorrect since all persons would actually exist when uploading the same file a second time.
        assertThat(dto2.people.stream().allMatch(p -> p.isNew)).isTrue();
    }

    @Test
    public void batchUpdate_repetRawData_clearGroups_happyPath() throws Exception {
        //
        // MOCK STUFF
        //
        final var org = mockOrganization("Acme Inc.");

        when(dao.read(eq(org), anyString())).thenThrow(new ObjectNotFoundException());
        final var mockedImportedPerson = mockPerson(org, "Person");
        when(dao.create(any(Organization.class), any())).thenReturn(mockedImportedPerson);

        final var groupTracker = mockGroup(org, "Sp\u00e5rare");
        when(groupsDao.read(eq(org), eq("Sp\u00e5rare"))).thenReturn(groupTracker);

        final var groupDiscoverer = mockGroup(org, "Uppt\u00e4ckare");
        when(groupsDao.read(eq(org), eq("Uppt\u00e4ckare"))).thenReturn(groupDiscoverer);

        final var groupAdventurer = mockGroup(org, "Ton\u00e5r");
        when(groupsDao.create(eq(org), any())).thenReturn(groupAdventurer);
        when(groupsDao.read(eq(org), eq("Ton\u00e5r"))).thenThrow(new ObjectNotFoundException());

        final var trackerInImport = mockPerson(org, "Edla Backman");
        final var trackerNotInImport = mockPerson(org, "OLD TRACKER");
        final var discovererInImport = mockPerson(org, "B-A", "abrahamsson-boel");
        final var discovererNotInImport = mockPerson(org, "OLD DISCOVERER");
        when(membershipsDao.getMemberships(eq(groupTracker))).thenReturn(Arrays.asList(
                new GroupMembership(groupTracker, trackerInImport, GroupRole.MEMBER),
                new GroupMembership(groupTracker, trackerNotInImport, GroupRole.MEMBER)
        ));
        when(membershipsDao.getMemberships(eq(groupDiscoverer))).thenReturn(Arrays.asList(
                new GroupMembership(groupDiscoverer, discovererInImport, GroupRole.MEMBER),
                new GroupMembership(groupDiscoverer, discovererNotInImport, GroupRole.MEMBER)
        ));

        //
        // Upload an XML file with data from Repet.
        //

        final var multiPartReq1 = (FormDataMultiPart) new FormDataMultiPart()
                .field("importClearGroups", "true")
                .field("importRawData", Resources.toString(Resources.getResource("batchupsert-repet-narvarolista.xml"), Charsets.UTF_8));
        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MultiPartFeature.class)
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.entity(multiPartReq1, multiPartReq1.getMediaType()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        verify(groupsDao, times(16 + 1)).read(eq(org), eq("Sp\u00e5rare"));
        verify(groupsDao, times(15 + 1)).read(eq(org), eq("Uppt\u00e4ckare"));
        verify(membershipsDao).getMemberships(eq(groupTracker));
        verify(membershipsDao).getMemberships(eq(groupDiscoverer));
        verify(membershipsDao, never()).remove(eq(trackerInImport), eq(groupTracker));
        verify(membershipsDao).remove(eq(trackerNotInImport), eq(groupTracker));
        verify(membershipsDao, never()).remove(eq(discovererInImport), eq(groupDiscoverer));
        verify(membershipsDao).remove(eq(discovererNotInImport), eq(groupDiscoverer));
    }

    @Test
    public void batchUpdate_repet_randomDataShouldNotBeAccepted() throws Exception {
        final var org = mockOrganization("Acme Inc.");

        final var multiPartReq1 = (FormDataMultiPart) new FormDataMultiPart()
                .bodyPart(new FormDataBodyPart("importFile", new RandomStringGenerator.Builder().build().generate(100)));
        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MultiPartFeature.class)
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.entity(multiPartReq1, multiPartReq1.getMediaType()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(dao, never()).update(any(Integer.class), any(PersonProperties.class));
    }

    @Test
    public void batchUpdate_repet_emptyXmlFileShouldNotBeAccepted() throws Exception {
        final var org = mockOrganization("Acme Inc.");

        final var fileDataBodyPart = new FileDataBodyPart(
                "importFile",
                new File(getClass().getClassLoader().getResource("batchupsert-repet-narvarolista-empty.xml").getFile()));
        final var multiPartReq1 = (FormDataMultiPart) new FormDataMultiPart()
                .bodyPart(fileDataBodyPart);
        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MultiPartFeature.class)
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.entity(multiPartReq1, multiPartReq1.getMediaType()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(dao, never()).update(any(Integer.class), any(PersonProperties.class));
    }

    @Test
    public void batchUpdate_repet_xmlBombShouldNotBeAccepted() throws Exception {
        final var org = mockOrganization("Acme Inc.");

        final var fileDataBodyPart = new FileDataBodyPart(
                "importFile",
                new File(getClass().getClassLoader().getResource("xml-bomb.xml").getFile()));
        final var multiPartReq1 = (FormDataMultiPart) new FormDataMultiPart()
                .bodyPart(fileDataBodyPart);
        final var response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/people")
                .register(MultiPartFeature.class)
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.entity(multiPartReq1, multiPartReq1.getMediaType()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        verify(dao, never()).create(any(Organization.class), any(PersonProperties.class));
        verify(dao, never()).update(any(Integer.class), any(PersonProperties.class));
    }

    @Test
    public void batchUpdate_csv_updateSelf_expectBadRequest() throws Exception {
        final var org = mockOrganization("org");

        // Some mocking which maybe should be moved to MockUtil
        final var editorPerson = credentialsDao.get(CredentialsType.PASSWORD, USERNAME_EDITOR).getPerson();
        final var editorOrganization = editorPerson.getOrganization();
        when(organizationsDao.read(eq(editorOrganization.getId()))).thenReturn(editorOrganization);

        //SUT 1: Batch update using custom identifier
        when(dao.read(any(Organization.class), eq("alice_editor"))).thenReturn(editorPerson);

        final var response1 = resources
                .target("/organizations/" + UuidString.toString(editorOrganization.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.entity("" +
                                "name,email,custom_identifier,id\n" +
                                "Alicia,alice@example.com,alice_editor,",
                        "text/csv"
                ));

        assertThat(response1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        //SUT 1: Batch update using primary key value
        when(dao.read(eq(editorPerson.getId()))).thenReturn(editorPerson);

        final var response2 = resources
                .target("/organizations/" + UuidString.toString(editorOrganization.getId()) + "/people")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .put(Entity.entity("" +
                                "name,email,custom_identifier,id\n" +
                                "Alicia,alice@example.com,," + editorPerson.getId(),
                        "text/csv"
                ));

        assertThat(response2.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(dao, never()).create(eq(org), any(PersonProperties.class));
        verify(dao, never()).update(anyInt(), any(PersonProperties.class));
        verify(dao, never()).delete(anyInt());
    }

    @Test
    public void achievementSummary_twoAchievementTwoSteps_successful() throws Exception {
        final var org = mockOrganization("Alice's Organization");
        final var person1 = mockPerson(org, "Alice");
        final var person2 = mockPerson(org, "Bob");
        final var person3 = mockPerson(org, "Carol");
        final var a1p1 = mockProgress(true, person1);
        final var a1p2 = mockProgress(true, person2);
        final var a1p3 = mockProgress(true, person1);
        final var a1p4 = mockProgress(true, person2);
        final var a1s1 = mockStep(a1p1, a1p2);
        final var a1s2 = mockStep(a1p3, a1p4);
        final var a1 = mockAchievement("Climb mountain", a1s1, a1s2);

        final var a2p1 = mockProgress(false, person2);
        final var a2p2 = mockProgress(true, person1);
        final var a2p3 = mockProgress(false, person2);
        final var a2p4 = mockProgress(false, person1);
        final var s2 = mockStep(a2p1, a2p2);
        final var s3 = mockStep(a2p3, a2p4);
        final var a2 = mockAchievement("Cook egg", s2, s3);

        when(achievementsDao.findWithProgressForPerson(eq(person1)))
                .thenReturn(Arrays.asList(a1, a2));

        final var dto = resources.client()
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
        final var person1 = MockUtil.mockPerson(org, name, customId, Roles.READER);
        when(dao.read(eq(person1.getId()))).thenReturn(person1);
        final var passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
        final var credentials = new Credentials("username", passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData());
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(name))).thenReturn(credentials);
        return person1;
    }

/*
    private Group mockGroup(Organization org, String name) throws ObjectNotFoundException, IOException {
        final Group grp = MockUtil.mockGroup(org, name);
        when(dao.read(eq(person1.getId()))).thenReturn(person1);
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
        final Credentials credentials = new Credentials("username", passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData());
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(name))).thenReturn(credentials);
        return person1;
    }
*/
}