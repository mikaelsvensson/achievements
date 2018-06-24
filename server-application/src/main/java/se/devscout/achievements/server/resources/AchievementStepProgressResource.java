package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.ProgressDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProgressProperties;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.filter.audit.Audited;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("achievements/{achievementId}/steps/{stepId}/progress")
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
    @Path("{personId}")
    public ProgressDTO get(@PathParam("achievementId") UuidString achievementId,
                           @PathParam("stepId") Integer stepId,
                           @PathParam("personId") Integer personId,
                           @Auth User user) {
        try {
            final AchievementStep step = stepsDao.read(stepId);
            verifyParent(achievementId.getUUID(), step);
            final Person person = peopleDao.read(personId);
            return map(dao.get(step, person), ProgressDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    //FIXME: Should probably be PUT instead of POST
    @POST
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Audited(logRequest = true)
    @Path("{personId}")
    public ProgressDTO set(@PathParam("achievementId") UuidString achievementId,
                           @PathParam("stepId") Integer stepId,
                           @PathParam("personId") Integer personId,
                           @Auth User user,
                           ProgressDTO dto) {
        try {
            final AchievementStep step = stepsDao.read(stepId);
            verifyParent(achievementId.getUUID(), step);
            final Person person = peopleDao.read(personId);
            return map(dao.set(step, person, new AchievementStepProgressProperties(dto.completed, dto.note)), ProgressDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @DELETE
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Audited(logRequest = true)
    @Path("{personId}")
    public Response unset(@PathParam("achievementId") UuidString achievementId,
                          @PathParam("stepId") Integer stepId,
                          @PathParam("personId") Integer personId,
                          @Auth User user) {
        try {
            final AchievementStep step = stepsDao.read(stepId);
            verifyParent(achievementId.getUUID(), step);
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
