package se.devscout.achievements.server;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.AchievementStepProgressDao;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.AchievementsResource;
import se.devscout.achievements.server.resources.UuidString;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static se.devscout.achievements.server.MockUtil.mockOrganization;
import static se.devscout.achievements.server.MockUtil.mockPerson;

public class AchievementsResourceTest {

    private final AchievementsDao dao = mock(AchievementsDao.class);
    private final AchievementStepProgressDao progressDao = mock(AchievementStepProgressDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);
    private final PeopleDao peopleDao = mock(PeopleDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new AchievementsResource(dao, progressDao))
            .build();

    @Before
    public void setUp() throws Exception {
        //TODO: Put this mocking into some shared class:
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
        final Organization organization = mockOrganization("Acme Inc.");
        final Person person = mockPerson(organization, "Alice");
        final Credentials credentials = new Credentials("username", passwordValidator.getIdentityProvider(), passwordValidator.getSecret(), person);
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("user"))).thenReturn(credentials);
    }

    @Test
    public void get_happyPath() throws Exception {
        final Achievement achievement = mock(Achievement.class);
        final UUID uuid = UUID.randomUUID();
        when(achievement.getId()).thenReturn(uuid);
        when(dao.read(eq(uuid))).thenReturn(achievement);
        final Response response = resources
                .target("/achievements/" + UuidString.toString(uuid))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        final AchievementDTO dto = response.readEntity(AchievementDTO.class);
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
        final Response response = resources
                .target("/achievements")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        final ArrayNode dto = response.readEntity(ArrayNode.class);
        assertThat(dto.size()).isEqualTo(3);
        assertThat(dto.get(0).has("id")).isTrue();
        assertThat(dto.get(0).has("name")).isTrue();
        assertThat(dto.get(0).has("tags")).isTrue();
        assertThat(dto.get(0).has("description")).isFalse();
        assertThat(dto.get(0).has("steps")).isFalse();
    }

    private Achievement mockAchievement(String name) {
        final Achievement achievement = mock(Achievement.class);
        when(achievement.getId()).thenReturn(UUID.randomUUID());
        when(achievement.getName()).thenReturn(name);
        when(achievement.getDescription()).thenReturn(RandomStringUtils.randomAlphabetic(1000));
        final AchievementStep step = mock(AchievementStep.class);
        when(step.getDescription()).thenReturn(RandomStringUtils.randomAlphabetic(100));
        when(achievement.getSteps()).thenReturn(Collections.singletonList(step));
        return achievement;
    }

    @Test
    public void get_notFound() throws Exception {
        when(dao.read(any(UUID.class))).thenThrow(new NotFoundException());
        final Response response = resources
                .target("/achievements/id")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void delete_notFound() throws Exception {
        doThrow(new NotFoundException()).when(dao).delete(any(UUID.class));
        final Response response = resources
                .target("/achievements/id")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void delete_happyPath() throws Exception {
        final Achievement achievement = mock(Achievement.class);
        final UUID uuid = UUID.randomUUID();
        when(achievement.getId()).thenReturn(uuid);
        when(dao.read(eq(uuid))).thenReturn(achievement);
        final Response response = resources
                .target("/achievements/" + UuidString.toString(uuid))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    }

    @Test
    public void create_happyPath() throws Exception {
        final Achievement achievement = mock(Achievement.class);
        when(achievement.getId()).thenReturn(UUID.randomUUID());
        when(achievement.getName()).thenReturn("abc");
        when(dao.create(any(AchievementProperties.class))).thenReturn(achievement);
        final Response response = resources
                .target("/achievements")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .post(Entity.json(new AchievementDTO()));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final AchievementDTO dto = response.readEntity(AchievementDTO.class);
        assertThat(response.getLocation().getPath()).isEqualTo("/achievements/" + UuidString.toString(achievement.getId()));
        assertThat(dto.id).isEqualTo(UuidString.toString(achievement.getId()));
        assertThat(dto.name).isEqualTo("abc");
    }

}