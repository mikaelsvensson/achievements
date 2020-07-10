package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.api.PersonBaseDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static se.devscout.achievements.server.MockUtil.*;

public class AchievementsResourceTest {

    private final AchievementsDao dao = mock(AchievementsDao.class);
    private final AchievementStepProgressDao progressDao = mock(AchievementStepProgressDao.class);
    private final AuditingDao auditingDao = mock(AuditingDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);
    private final PeopleDao peopleDao = mock(PeopleDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new AchievementsResource(dao, progressDao, auditingDao, peopleDao))
            .build();

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
    }

    @Test
    public void get_happyPath() throws Exception {
        final var achievement = mock(Achievement.class);
        final var uuid = UUID.randomUUID();
        when(achievement.getId()).thenReturn(uuid);
        when(dao.read(eq(uuid))).thenReturn(achievement);
        final var response = resources
                .target("/achievements/" + UuidString.toString(uuid))
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        final var dto = response.readEntity(AchievementDTO.class);
        assertThat(dto.id).isEqualTo(UuidString.toString(achievement.getId()));
    }

    @Test
    public void getAll_happyPath() throws Exception {
        final List<Achievement> all = Lists.newArrayList(
                mockAchievement("Learn to ride a bike"),
                mockAchievement("Learn to ride a motorcycle"),
                mockAchievement("Learn to drive a car")
        );
        when(dao.readAll()).thenReturn(all);
        final var response = resources
                .target("/achievements")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        final var dto = response.readEntity(ArrayNode.class);
        assertThat(dto.size()).isEqualTo(3);
        assertThat(dto.get(0).has("id")).isTrue();
        assertThat(dto.get(0).has("name")).isTrue();
        assertThat(dto.get(0).has("tags")).isTrue();
        assertThat(dto.get(0).has("description")).isFalse();
        assertThat(dto.get(0).has("steps")).isFalse();
    }

    private Achievement mockAchievement(String name) {
        final var achievement = mock(Achievement.class);
        when(achievement.getId()).thenReturn(UUID.randomUUID());
        when(achievement.getName()).thenReturn(name);
        when(achievement.getDescription()).thenReturn(RandomStringUtils.randomAlphabetic(1000));
        final var step = mock(AchievementStep.class);
        when(step.getDescription()).thenReturn(RandomStringUtils.randomAlphabetic(100));
        when(achievement.getSteps()).thenReturn(Collections.singletonList(step));
        return achievement;
    }

    @Test
    public void get_notFound() throws Exception {
        when(dao.read(any(UUID.class))).thenThrow(new NotFoundException());
        final var response = resources
                .target("/achievements/id")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void delete_notFound() throws Exception {
        doThrow(new NotFoundException()).when(dao).delete(any(UUID.class));
        final var response = resources
                .target("/achievements/id")
                .register(AUTH_FEATURE_ADMIN)
                .request()
                .delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void delete_happyPath() throws Exception {
        final var achievement = mock(Achievement.class);
        final var uuid = UUID.randomUUID();
        when(achievement.getId()).thenReturn(uuid);
        when(dao.read(eq(uuid))).thenReturn(achievement);
        final var response = resources
                .target("/achievements/" + UuidString.toString(uuid))
                .register(AUTH_FEATURE_ADMIN)
                .request()
                .delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    }

    @Test
    public void create_happyPath() throws Exception {
        final var achievement = mock(Achievement.class);
        when(achievement.getId()).thenReturn(UUID.randomUUID());
        when(achievement.getName()).thenReturn("abc");
        when(dao.create(any(AchievementProperties.class))).thenReturn(achievement);
        final var response = resources
                .target("/achievements")
                .register(AUTH_FEATURE_ADMIN)
                .request()
                .post(Entity.json(new AchievementDTO()));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final var dto = response.readEntity(AchievementDTO.class);
        assertThat(response.getLocation().getPath()).isEqualTo("/achievements/" + UuidString.toString(achievement.getId()));
        assertThat(dto.id).isEqualTo(UuidString.toString(achievement.getId()));
        assertThat(dto.name).isEqualTo("abc");
    }

    @Test
    public void awards_get_happyPath() throws ObjectNotFoundException {
        final var mockedReader = credentialsDao.get(CredentialsType.PASSWORD, USERNAME_READER).getPerson();
        when(peopleDao.read(eq(mockedReader.getId()))).thenReturn(mockedReader);

        final var achievement = mockAchievement("The Achievement");
        when(dao.read(eq(achievement.getId()))).thenReturn(achievement);

        final var person = mockPerson(mockedReader.getOrganization(), "Carol", Roles.READER);
        when(peopleDao.read(eq(person.getId()))).thenReturn(person);

        when(peopleDao.getByAwardedAchievement(eq(mockedReader.getOrganization()), eq(achievement))).thenReturn(Collections.singletonList(person));

        final var response = resources
                .target("/achievements/" + UuidString.toString(achievement.getId()) + "/awards")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final List<PersonBaseDTO> dto = response.readEntity(new GenericType<>() {
        });
        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).name).isEqualTo("Carol");
    }

    @Test
    public void awards_get_achievementNotFound() throws ObjectNotFoundException {
        final var mockedReader = credentialsDao.get(CredentialsType.PASSWORD, USERNAME_READER).getPerson();
        when(peopleDao.read(eq(mockedReader.getId()))).thenReturn(mockedReader);

        final var achievement = mockAchievement("The Achievement");
        when(dao.read(eq(achievement.getId()))).thenThrow(new ObjectNotFoundException());

        final var response = resources
                .target("/achievements/" + UuidString.toString(achievement.getId()) + "/awards")
                .register(MockUtil.AUTH_FEATURE_READER)
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);

        verify(peopleDao, never()).getByAwardedAchievement(any(Organization.class), any(Achievement.class));
    }

    @Test
    public void awards_add_happyPath() throws ObjectNotFoundException {
        final var mockedEditor = credentialsDao.get(CredentialsType.PASSWORD, USERNAME_EDITOR).getPerson();

        final var organization = mockedEditor.getOrganization();
        final var achievement = mockAchievement("The Achievement");
        final var person = mockPerson(organization, "Alice", Roles.EDITOR);

        when(peopleDao.read(eq(mockedEditor.getId()))).thenReturn(mockedEditor);
        when(peopleDao.read(eq(person.getId()))).thenReturn(person);
        when(dao.read(eq(achievement.getId()))).thenReturn(achievement);

        final var response = resources
                .target("/achievements/" + UuidString.toString(achievement.getId()) + "/awards/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(null);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
        verify(dao).addAwardedTo(eq(achievement), eq(person));
    }

    @Test
    public void awards_add_otherOrganization() throws ObjectNotFoundException {
        final var mockedEditor = credentialsDao.get(CredentialsType.PASSWORD, USERNAME_EDITOR).getPerson();

        final var otherOrg = mockOrganization("Other Org");
        final var achievement = mockAchievement("The Achievement");
        final var person = mockPerson(otherOrg, "Carol", Roles.EDITOR);

        when(peopleDao.read(eq(mockedEditor.getId()))).thenReturn(mockedEditor);
        when(peopleDao.read(eq(person.getId()))).thenReturn(person);

        final var response = resources
                .target("/achievements/" + UuidString.toString(achievement.getId()) + "/awards/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .post(null);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        verify(dao, never()).addAwardedTo(any(Achievement.class), any(Person.class));

    }

    @Test
    public void awards_delete_happyPath() throws ObjectNotFoundException {
        final var mockedEditor = credentialsDao.get(CredentialsType.PASSWORD, USERNAME_EDITOR).getPerson();

        final var organization = mockedEditor.getOrganization();
        final var achievement = mockAchievement("The Achievement");
        final var person = mockPerson(organization, "Alice", Roles.EDITOR);

        when(peopleDao.read(eq(mockedEditor.getId()))).thenReturn(mockedEditor);
        when(peopleDao.read(eq(person.getId()))).thenReturn(person);
        when(dao.read(eq(achievement.getId()))).thenReturn(achievement);

        final var response = resources
                .target("/achievements/" + UuidString.toString(achievement.getId()) + "/awards/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
        verify(dao).removeAwardedTo(eq(achievement), eq(person));
    }

    @Test
    public void awards_delete_otherOrganization() throws ObjectNotFoundException {
        final var mockedEditor = credentialsDao.get(CredentialsType.PASSWORD, USERNAME_EDITOR).getPerson();

        final var otherOrg = mockOrganization("Other Org");
        final var achievement = mockAchievement("The Achievement");
        final var person = mockPerson(otherOrg, "Carol", Roles.EDITOR);

        when(peopleDao.read(eq(mockedEditor.getId()))).thenReturn(mockedEditor);
        when(peopleDao.read(eq(person.getId()))).thenReturn(person);

        final var response = resources
                .target("/achievements/" + UuidString.toString(achievement.getId()) + "/awards/" + person.getId())
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        verify(dao, never()).removeAwardedTo(any(Achievement.class), any(Person.class));

    }

}