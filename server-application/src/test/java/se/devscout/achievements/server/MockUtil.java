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
    public static final HttpAuthenticationFeature AUTH_FEATURE_EDITOR = HttpAuthenticationFeature.basic(USERNAME_EDITOR, "password");
    public static final HttpAuthenticationFeature AUTH_FEATURE_READER = HttpAuthenticationFeature.basic(USERNAME_READER, "password");

    public static Organization mockOrganization(String name) {
        final UUID uuid = UUID.randomUUID();

        final Organization orgA = mock(Organization.class);
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
        final Person person = mock(Person.class);
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

    private static int getRandomNonZeroValue() {
        return new Random().nextInt(10000) + 1;
    }

    public static Achievement mockAchievement(String name, AchievementStep... steps) {
        final Achievement achievementMock = mock(Achievement.class);
        when(achievementMock.getName()).thenReturn(name);
        when(achievementMock.getSteps()).thenReturn(Arrays.asList(
                steps
        ));
        return achievementMock;
    }

    public static AchievementStep mockStep(AchievementStepProgress... progress) {
        final AchievementStep stepMock = mock(AchievementStep.class);
        when(stepMock.getProgressList()).thenReturn(Arrays.asList(progress));
        return stepMock;
    }

    public static AchievementStepProgress mockProgress(boolean completed, Person person) {
        final AchievementStepProgress progressMock = mock(AchievementStepProgress.class);
        when(progressMock.isCompleted()).thenReturn(completed);
        when(progressMock.getPerson()).thenReturn(person);
        return progressMock;
    }

    public static void setupDefaultCredentials(CredentialsDao credentialsDao) throws ObjectNotFoundException {
        {
            final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
            final Organization organization = mockOrganization("Acme Inc.");
            final Person person = mockPerson(organization, "Alice Reader", "alice_reader", Roles.READER);
            final Credentials credentials = new Credentials(USERNAME_READER, passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData(), person);
            credentials.setId(UUID.randomUUID());
            when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(USERNAME_READER))).thenReturn(credentials);
            when(credentialsDao.read(eq(credentials.getId()))).thenReturn(credentials);
        }

        {
            final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
            final Organization organization = mockOrganization("Acme Inc.");
            final Person person = mockPerson(organization, "Alice Editor", "alice_editor", Roles.EDITOR);
            final Credentials credentials = new Credentials(USERNAME_EDITOR, passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData(), person);
            credentials.setId(UUID.randomUUID());
            when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(USERNAME_EDITOR))).thenReturn(credentials);
            when(credentialsDao.read(eq(credentials.getId()))).thenReturn(credentials);
        }
    }
}
