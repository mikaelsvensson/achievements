package se.devscout.achievements.server.resources;

import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.ProgressDTO;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;

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
    public AchievementStepProgress get(@PathParam("achievementId") UUID achievementId,
                                       @PathParam("stepId") String stepId,
                                       @PathParam("personId") String personId) {
        try {
            final AchievementStep step = stepsDao.get(stepId);
            verifyParent(achievementId, step);
            final Person person = peopleDao.get(personId);
            return dao.get(step, person);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @POST
    @UnitOfWork
    public AchievementStepProgress set(@PathParam("achievementId") UUID achievementId,
                                       @PathParam("stepId") String stepId,
                                       @PathParam("personId") String personId,
                                       ProgressDTO dto) {
        try {
            final AchievementStep step = stepsDao.get(stepId);
            verifyParent(achievementId, step);
            final Person person = peopleDao.get(personId);
            return dao.set(step, person, new AchievementStepProgressProperties(dto.completed, dto.note));
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @DELETE
    @UnitOfWork
    public Response unset(@PathParam("achievementId") UUID achievementId,
                                        @PathParam("stepId") String stepId,
                                        @PathParam("personId") String personId) {
        try {
            final AchievementStep step = stepsDao.get(stepId);
            verifyParent(achievementId, step);
            final Person person = peopleDao.get(personId);
            dao.unset(step, person);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    private void verifyParent(UUID achievementId, AchievementStep step) throws ObjectNotFoundException {
        Achievement achievement = achievementDao.get(achievementId.toString());
        if (!step.getAchievement().getId().equals(achievement.getId())) {
            throw new NotFoundException();
        }
    }
}
