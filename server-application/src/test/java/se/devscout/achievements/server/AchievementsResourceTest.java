package se.devscout.achievements.server;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.data.dao.AchievementStepProgressDao;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;
import se.devscout.achievements.server.resources.AchievementsResource;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AchievementsResourceTest {

    private final AchievementsDao dao = mock(AchievementsDao.class);
    private final AchievementStepProgressDao progressDao = mock(AchievementStepProgressDao.class);

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new AchievementsResource(dao, progressDao))
            .build();

    @Test
    public void get_happyPath() throws Exception {
        final Achievement achievement = mock(Achievement.class);
        final UUID uuid = UUID.randomUUID();
        when(achievement.getId()).thenReturn(uuid);
        when(dao.read(eq(uuid))).thenReturn(achievement);
        final Response response = resources
                .target("/achievements/" + uuid.toString())
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        final AchievementDTO dto = response.readEntity(AchievementDTO.class);
        assertThat(dto.id).isEqualTo(achievement.getId().toString());
    }

    @Test
    public void get_notFound() throws Exception {
        when(dao.read(any(UUID.class))).thenThrow(new NotFoundException());
        final Response response = resources
                .target("/achievements/id")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void delete_notFound() throws Exception {
        doThrow(new NotFoundException()).when(dao).delete(any(UUID.class));
        final Response response = resources
                .target("/achievements/id")
                .request()
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
                .target("/achievements/" + uuid.toString())
                .request()
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
                .post(Entity.json(new AchievementDTO()));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final AchievementDTO dto = response.readEntity(AchievementDTO.class);
        assertThat(response.getLocation().getPath()).isEqualTo("/achievements/" + dto.id);
        assertThat(dto.id).isEqualTo(achievement.getId().toString());
        assertThat(dto.name).isEqualTo("abc");
    }

}