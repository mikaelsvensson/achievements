package se.devscout.achievements.server.resources;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.api.PersonProfileDTO;
import se.devscout.achievements.server.auth.jwt.JwtSignInToken;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class MyResourceTest {

    private static final int ZERO = 0;
    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final GroupsDao groupsDao = mock(GroupsDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);
    private final AchievementsDao achievementsDao = mock(AchievementsDao.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final JwtSignInTokenService signInTokenService = mock(JwtSignInTokenService.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new MyResource(peopleDao, groupsDao, achievementsDao, credentialsDao, emailSender, URI.create("http://gui/"), signInTokenService))
            .build();

    @Test
    public void getMyPeople_happyPath() throws Exception {
        final Organization org1 = mockOrganization("Monsters Inc.");
        final Person person1 = mockPerson(org1, "Alice");
        final Organization org2 = mockOrganization("Cyberdyne Systems");
        final Person person2a = mockPerson(org2, "Bob");
        final Person person2b = mockPerson(org2, "Carol");
        mockRegularPasswordCredentials(person2a);

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
        mockRegularPasswordCredentials(person);

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

    @Test
    public void setPassword_happyPath() throws ObjectNotFoundException, DaoException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("/my/password/")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("bob:pw".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new SetPasswordDTO("pw", "new_password", "new_password")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(credentialsDao).update(any(UUID.class), any(CredentialsProperties.class));
    }

    @Test
    public void setPassword_incorrectCurrentPassword() throws ObjectNotFoundException, DaoException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("/my/password/")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("bob:pw".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new SetPasswordDTO("bad_pw", "new_password", "new_password")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(credentialsDao, never()).update(any(UUID.class), any(CredentialsProperties.class));
    }

    @Test
    public void setPassword_missingCurrentPasswordBasicAuth() throws ObjectNotFoundException, DaoException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("/my/password/")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("bob:pw".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new SetPasswordDTO(null, "new_password", "new_password")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(credentialsDao, never()).update(any(UUID.class), any(CredentialsProperties.class));
    }

    @Test
    public void setPassword_forgotPasswordWorkflowUsingOnetimePassword() throws ObjectNotFoundException, DaoException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Set<Credentials> list = Sets.newHashSet();
        final Person person = mockPerson(org, "Bob");
        {
            // Mock credentials to change
            final PasswordValidator passwordValidator = new PasswordValidator(
                    SecretGenerator.PDKDF2,
                    "pw".toCharArray());
            final Credentials credentials = new Credentials(
                    "bob",
                    passwordValidator.getCredentialsType(),
                    passwordValidator.getCredentialsData());
            credentials.setId(UUID.randomUUID());
            credentials.setPerson(person);
            list.add(credentials);
            when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq("bob"))).thenReturn(credentials);
        }
        {
            // Mock one-time credentials necessary to change password without specifying the current password
            final Credentials credentials = new Credentials(
                    "onetimepassword",
                    CredentialsType.ONETIME_PASSWORD,
                    null);
            credentials.setId(UUID.randomUUID());
            credentials.setPerson(person);
            list.add(credentials);
            when(credentialsDao.get(eq(CredentialsType.ONETIME_PASSWORD), eq("onetimepassword"))).thenReturn(credentials);
        }
        when(person.getCredentials()).thenReturn(list);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));
        when(signInTokenService.encode(any(JwtSignInToken.class))).thenReturn("token");

        final Response response = resources
                .target("/my/password/")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "OneTime " + base64("onetimepassword"))
                .post(Entity.json(new SetPasswordDTO(null, "new_password", "new_password")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final AuthTokenDTO responseDTO = response.readEntity(AuthTokenDTO.class);
        assertThat(responseDTO.token).isEqualTo("token");

        verify(credentialsDao).update(any(UUID.class), any(CredentialsProperties.class));
    }

    @Test
    public void sendSetPasswordLink_userIsSignedIn() throws ObjectNotFoundException, EmailSenderException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("my/send-set-password-link")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("bob:pw".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new ForgotPasswordDTO("user@example.com")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(emailSender).send(anyString(), eq("user@example.com"), anyString(), anyString());
    }

    @Test
    public void sendSetPasswordLink_otherUsersEmailAddress() throws ObjectNotFoundException, EmailSenderException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("my/send-set-password-link")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("bob:pw".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new ForgotPasswordDTO("trudy@example.com")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(emailSender).send(anyString(), eq("user@example.com"), anyString(), anyString());
    }

    @Test
    public void sendSetPasswordLink_userIsNotSignedIn() throws ObjectNotFoundException, EmailSenderException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("my/send-set-password-link")
                .request()
                .post(Entity.json(new ForgotPasswordDTO("user@example.com")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(emailSender).send(anyString(), eq("user@example.com"), anyString(), anyString());
    }

    @Test
    public void sendSetPasswordLink_incorrectEmail() throws ObjectNotFoundException, EmailSenderException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("my/send-set-password-link")
                .request()
                .post(Entity.json(new ForgotPasswordDTO("trudy@example.com")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void sendSetPasswordLink_failedToSendEmail() throws ObjectNotFoundException, EmailSenderException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        doThrow(new EmailSenderException("Boom")).when(emailSender).send(anyString(), anyString(), anyString(), anyString());

        final Response response = resources
                .target("my/send-set-password-link")
                .request()
                .post(Entity.json(new ForgotPasswordDTO("user@example.com")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(emailSender).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void setPassword_nonmatchingPassword() throws ObjectNotFoundException, DaoException {
        final Organization org = mockOrganization("Cyberdyne Systems");
        final Person person = mockPerson(org, "Bob");
        mockRegularPasswordCredentials(person);

        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org));

        final Response response = resources
                .target("/my/password/")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("bob:pw".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new SetPasswordDTO("bad_pw", "new_password1", "new_password2")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        verify(credentialsDao, never()).update(any(UUID.class), any(CredentialsProperties.class));
    }

    private String base64(String str) {
        return BaseEncoding.base64().encode(str.getBytes(Charsets.UTF_8));
    }

    private void mockRegularPasswordCredentials(Person person) throws ObjectNotFoundException {
        final PasswordValidator passwordValidator = new PasswordValidator(
                SecretGenerator.PDKDF2,
                "pw".toCharArray());
        final Credentials credentials = new Credentials(
                "bob",
                passwordValidator.getCredentialsType(),
                passwordValidator.getCredentialsData());
        credentials.setId(UUID.randomUUID());
        credentials.setPerson(person);
        when(person.getCredentials()).thenReturn(Collections.singleton(credentials));
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq("bob"))).thenReturn(credentials);
    }

    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException {
        final Integer uuid = getRandomNonZeroValue();

        final String email = "user@example.com";

        final Person person = mock(Person.class);
        when(person.getId()).thenReturn(uuid);
        when(person.getOrganization()).thenReturn(org);
        when(person.getName()).thenReturn(name);
        when(person.getEmail()).thenReturn(email);

        when(peopleDao.read(eq(uuid))).thenReturn(person);
        when(peopleDao.getByEmail(eq(email))).thenReturn(Collections.singletonList(person));

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