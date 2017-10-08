package se.devscout.achievements.server.resources;

import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.ProgressDTO;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProgressProperties;
import se.devscout.achievements.server.data.model.Person;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/achievements/{achievementId}/steps/{stepId}/person/{personId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AchievementStepProgressResource extends AbstractResource {
    private AchievementStepsDao stepsDao;
    private AchievementsDao achievementDao;
    private PeopleDao peopleDao;
    private AchievementStepProgressDao dao;

    public AchievementStepProgressResource(AchievementStepsDao stepsDao, AchievementsDao achievementDao, PeopleDao peopleDao, AchievementStepProgressDao dao) {
        this.stepsDao = stepsDao;
        this.achievementDao = achievementDao;
        this.peopleDao = peopleDao;
        this.dao = dao;
    }

    @GET
    @UnitOfWork
    public ProgressDTO get(@PathParam("achievementId") UUID achievementId,
                                       @PathParam("stepId") Integer stepId,
                                       @PathParam("personId") Integer personId) {
        try {
            final AchievementStep step = stepsDao.read(stepId);
            verifyParent(achievementId, step);
            final Person person = peopleDao.read(personId);
            return map(dao.get(step, person), ProgressDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @POST
    @UnitOfWork
    public ProgressDTO set(@PathParam("achievementId") UUID achievementId,
                                       @PathParam("stepId") Integer stepId,
                                       @PathParam("personId") Integer personId,
                                       ProgressDTO dto) {
        try {
            final AchievementStep step = stepsDao.read(stepId);
            verifyParent(achievementId, step);
            final Person person = peopleDao.read(personId);
            return map(dao.set(step, person, new AchievementStepProgressProperties(dto.completed, dto.note)), ProgressDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @DELETE
    @UnitOfWork
    public Response unset(@PathParam("achievementId") UUID achievementId,
                                        @PathParam("stepId") Integer stepId,
                                        @PathParam("personId") Integer personId) {
        try {
            final AchievementStep step = stepsDao.read(stepId);
            verifyParent(achievementId, step);
            final Person person = peopleDao.read(personId);
            dao.unset(step, person);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    private void verifyParent(UUID achievementId, AchievementStep step) throws ObjectNotFoundException {
        Achievement achievement = achievementDao.read(achievementId);
        if (!step.getAchievement().getId().equals(achievement.getId())) {
            throw new NotFoundException();
        }
    }
}
