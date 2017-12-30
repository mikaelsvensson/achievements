package se.devscout.achievements.server;

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
    public static Organization mockOrganization(String name) {
        final UUID uuid = UUID.randomUUID();

        final Organization orgA = mock(Organization.class);
        when(orgA.getId()).thenReturn(uuid);
        when(orgA.getName()).thenReturn(name);
        return orgA;
    }

    public static Person mockPerson(Organization organization, String name) {
        return mockPerson(organization, name, null);
    }

    public static Person mockPerson(Organization organization, String name, String customId) {
        final Integer id = getRandomNonZeroValue();
        final Person person = mock(Person.class);
        when(person.getId()).thenReturn(id);
        when(person.getName()).thenReturn(name);
        when(person.getCustomIdentifier()).thenReturn(customId);
        if (organization != null) {
            when(person.getOrganization()).thenReturn(organization);
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
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
        final Organization organization = mockOrganization("Acme Inc.");
        final Person person = mockPerson(organization, "Alice");
        final Credentials credentials = new Credentials("username", passwordValidator.getCredentialsType(), passwordValidator.getCredentialsData(), person);
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq("user"))).thenReturn(credentials);
    }
}
