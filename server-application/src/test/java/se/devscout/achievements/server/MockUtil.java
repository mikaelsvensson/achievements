package se.devscout.achievements.server;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.*;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockUtil {

    public static final String USERNAME_EDITOR = "username_editor";
    public static final String USERNAME_READER = "username_reader";
    public static final String USERNAME_ADMIN = "username_admin";
    public static final HttpAuthenticationFeature AUTH_FEATURE_EDITOR = HttpAuthenticationFeature.basic(USERNAME_EDITOR, "password");
    public static final HttpAuthenticationFeature AUTH_FEATURE_READER = HttpAuthenticationFeature.basic(USERNAME_READER, "password");
    public static final HttpAuthenticationFeature AUTH_FEATURE_ADMIN = HttpAuthenticationFeature.basic(USERNAME_ADMIN, "password");

    public static Organization mockOrganization(String name) {
        final var uuid = UUID.randomUUID();

        final var orgA = mock(Organization.class);
        when(orgA.getId()).thenReturn(uuid);
        when(orgA.getName()).thenReturn(name);
        return orgA;
    }

    public static Person mockPerson(Organization organization, String name, String role) {
        return mockPerson(organization, name, null, null, role);
    }

    public static Person mockPerson(Organization organization, String name, String customId, String role) {
        return mockPerson(organization, name, customId, null, role);
    }

    public static Person mockPerson(Organization organization, String name, String customId, String email, String role) {
        final Integer id = getRandomNonZeroValue();
        final var person = mock(Person.class);
        when(person.getId()).thenReturn(id);
        when(person.getName()).thenReturn(name);
        when(person.getCustomIdentifier()).thenReturn(customId);
        when(person.getRole()).thenReturn(role);
        if (organization != null) {
            when(person.getOrganization()).thenReturn(organization);
        }
        if (email != null) {
            when(person.getEmail()).thenReturn(email);
        }
        return person;
    }

    public static Group mockGroup(Organization org, String name) {
        final Integer id = getRandomNonZeroValue();
        final var mock = mock(Group.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getName()).thenReturn(name);
        when(mock.getOrganization()).thenReturn(org);
        return mock;
    }

    public static Credentials mockCredentials(Person person, String username) {
        final var passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());

        final var mock = mock(Credentials.class);
        when(mock.getPerson()).thenReturn(person);
        when(mock.getId()).thenReturn(UUID.randomUUID());
        when(mock.getData()).thenReturn(passwordValidator.getCredentialsData());
        when(mock.getType()).thenReturn(passwordValidator.getCredentialsType());
        when(mock.getUserId()).thenReturn(username);
        return mock;
    }

    private static int getRandomNonZeroValue() {
        return new Random().nextInt(10000) + 1;
    }

    public static Achievement mockAchievement(String name, AchievementStep... steps) {
        final var achievementMock = mock(Achievement.class);
        when(achievementMock.getId()).thenReturn(UUID.randomUUID());
        when(achievementMock.getName()).thenReturn(name);
        when(achievementMock.getSteps()).thenReturn(Arrays.asList(
                steps
        ));
        return achievementMock;
    }

    public static AchievementStep mockStep(AchievementStepProgress... progress) {
        final var stepMock = mock(AchievementStep.class);
        when(stepMock.getProgressList()).thenReturn(Arrays.asList(progress));
        return stepMock;
    }

    public static AchievementStepProgress mockProgress(boolean completed, Person person) {
        return mockProgress(completed ? AchievementStepProgressProperties.PROGRESS_COMPLETED : AchievementStepProgressProperties.PROGRESS_NOT_STARTED, person);
    }

    public static AchievementStepProgress mockProgress(Integer value, Person person) {
        final var progressMock = mock(AchievementStepProgress.class);
        when(progressMock.isCompleted()).thenReturn(value == AchievementStepProgressProperties.PROGRESS_COMPLETED);
        when(progressMock.getValue()).thenReturn(value);
        when(progressMock.getPerson()).thenReturn(person);
        return progressMock;
    }

    public static void setupDefaultCredentials(CredentialsDao credentialsDao) throws ObjectNotFoundException {
        {
            final var organization = mockOrganization("Acme Inc.");
            final var person = mockPerson(organization, "Alice Reader", "alice_reader", Roles.READER);
            final var credentials = mockCredentials(person, USERNAME_READER);
            when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(USERNAME_READER))).thenReturn(credentials);
            when(credentialsDao.read(eq(credentials.getId()))).thenReturn(credentials);
        }

        {
            final var organization = mockOrganization("Acme Inc.");
            final var person = mockPerson(organization, "Alice Editor", "alice_editor", Roles.EDITOR);
            final var credentials = mockCredentials(person, USERNAME_EDITOR);
            when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(USERNAME_EDITOR))).thenReturn(credentials);
            when(credentialsDao.read(eq(credentials.getId()))).thenReturn(credentials);
        }

        {
            final var organization = mockOrganization("Acme Inc.");
            final var person = mockPerson(organization, "Alice Admin", "alice_admin", Roles.ADMIN);
            final var credentials = mockCredentials(person, USERNAME_ADMIN);
            when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(USERNAME_ADMIN))).thenReturn(credentials);
            when(credentialsDao.read(eq(credentials.getId()))).thenReturn(credentials);
        }
    }

    public static GroupMembership mockMembership(Group group, Person person, GroupRole role) {
        final var membership = mock(GroupMembership.class);
        when(membership.getPerson()).thenReturn(person);
        when(membership.getGroup()).thenReturn(group);
        when(membership.getRole()).thenReturn(role);
        return membership;
    }
}
