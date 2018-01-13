package se.devscout.achievements.server.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.GroupBaseDTO;
import se.devscout.achievements.server.api.GroupMembershipDTO;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GroupMembershipsResourceTest {

    private static final int ZERO = 0;

    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);
    private final GroupsDao groupsDao = mock(GroupsDao.class);
    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final GroupMembershipsDao membershipsDao = mock(GroupMembershipsDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new GroupMembershipsResource(groupsDao, peopleDao, organizationsDao, membershipsDao))
            .build();

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
    }


    @Test
    public void add_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "Alice");
        final Group group = mockGroup(org, "The group");

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/groups/" + group.getId() + "/members/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(Entity.json(new GroupMembershipDTO(new GroupBaseDTO(group.getId(), null), new PersonBaseDTO(person.getId(), null), GroupRole.MANAGER)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(membershipsDao).add(eq(person), eq(group), eq(GroupRole.MANAGER));
    }

    private Group mockGroup(Organization org, String name) throws ObjectNotFoundException {
        final Group group = MockUtil.mockGroup(org, name);
        when(groupsDao.read(group.getId())).thenReturn(group);
        return group;
    }

    @Test
    public void getByGroup_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Person person = mockPerson(org, "Alice");
        final Group group = mockGroup(org, "The group");
        final GroupMembership membership = mockMembership(group, person);
        when(membershipsDao.getMemberships(eq(group))).thenReturn(Collections.singletonList(membership));

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/groups/" + group.getId() + "/members")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final List<GroupMembershipDTO> dto = response.readEntity(new GenericType<List<GroupMembershipDTO>>() {
        });
        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).group.id).isEqualTo(group.getId());
        assertThat(dto.get(0).group.name).isEqualTo(group.getName());
        assertThat(dto.get(0).person.id).isEqualTo(person.getId());
        assertThat(dto.get(0).person.name).isEqualTo(person.getName());
        assertThat(dto.get(0).role).isEqualTo(GroupRole.MEMBER);

        verify(membershipsDao).getMemberships(eq(group));
    }

    private Organization mockOrganization(String name) throws ObjectNotFoundException {
        final Organization org = MockUtil.mockOrganization(name);
        when(organizationsDao.read(eq(org.getId()))).thenReturn(org);
        return org;
    }

    @Test
    public void get_incorrectOrganization_expectNotFound() throws Exception {
        final Organization org = mockOrganization("org");
        final Group group = mockGroup(org, "The group");
        final UUID badId = UUID.randomUUID();
        when(organizationsDao.read(eq(badId))).thenThrow(new NotFoundException());

        final Response response = resources
                .target("/organizations/" + UuidString.toString(badId) + "/groups/" + group.getId() + "/members")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(membershipsDao, never()).getMemberships(any(Group.class));
        verify(organizationsDao).read(badId);
    }

    @Test
    public void delete_happyPath() throws Exception {
        final Organization org = mockOrganization("org");
        final Group group = mockGroup(org, "The group");
        final Person person = mockPerson(org, "Alice");
        final GroupMembership membership = mockMembership(group, person);
        when(membershipsDao.getMemberships(eq(group))).thenReturn(Collections.singletonList(membership));

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/groups/" + group.getId() + "/members/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(membershipsDao).remove(eq(person), eq(group));
    }

    @Test
    public void delete_unauthorizedUser_expectForbidden() throws Exception {
        final Organization org = mockOrganization("org");
        final Group group = mockGroup(org, "The group");
        final Person person = mockPerson(org, "Alice");
        final GroupMembership membership = mockMembership(group, person);
        when(membershipsDao.getMemberships(eq(group))).thenReturn(Collections.singletonList(membership));

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/groups/" + group.getId() + "/members/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(membershipsDao, never()).remove(any(), any());
    }

    @Test
    public void delete_wrongOrganization_expectNotFound() throws Exception {

        final Organization orgA = mockOrganization("orgA");
        final Organization orgB = mockOrganization("orgB");
        final Group group = mockGroup(orgA, "The group");
        final Person person = mockPerson(orgA, "Alice");
        final GroupMembership membership = mockMembership(group, person);

        final Response response = resources
                .target("/organizations/" + UuidString.toString(orgB.getId()) + "/groups/" + group.getId() + "/members/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(organizationsDao).read(eq(orgB.getId()));
        verify(membershipsDao, never()).remove(any(), any());
    }

    @Test
    public void get_wrongOrganization_expectNotFound() throws Exception {

        final Organization orgA = mockOrganization("org");
        final Organization orgB = mockOrganization("org");
        final Group group = mockGroup(orgA, "The group");
        final Person person = mockPerson(orgA, "Alice");
        final GroupMembership membership = mockMembership(group, person);

        final Response response = resources
                .target("/organizations/" + UuidString.toString(orgB.getId()) + "/groups/" + group.getId() + "/members")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(organizationsDao).read(eq(orgB.getId()));
    }

    @Test
    public void create_unauthorizedUser_expectForbidden() throws Exception {
        final Organization org = mockOrganization("org");
        final Group group = mockGroup(org, "The group");
        final Person person = mockPerson(org, "Alice");

        final Response response = resources
                .target("/organizations/" + UuidString.toString(org.getId()) + "/groups/" + group.getId() + "/members/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .post(Entity.json(new PersonDTO()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN_403);

        verify(membershipsDao, never()).add(any(), any(), any());
    }

    private GroupMembership mockMembership(Group group, Person person) {
        final GroupMembership membership = MockUtil.mockMembership(group, person, GroupRole.MEMBER);
        when(membershipsDao.getMemberships(eq(group))).thenReturn(Collections.singletonList(membership));
        return membership;
    }

    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException, IOException {
        return mockPerson(org, name, null);
    }

    private Person mockPerson(Organization org, String name, String customId) throws ObjectNotFoundException, IOException {
        final Person person1 = MockUtil.mockPerson(org, name, customId, Roles.READER);
        when(peopleDao.read(eq(person1.getId()))).thenReturn(person1);
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
        final Credentials credentials = new Credentials("username", passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData());
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(name))).thenReturn(credentials);
        return person1;
    }
}