package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.ProgressDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProgressProperties;
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
    private final AchievementStepsDao stepsDao;
    private final AchievementsDao achievementDao;
    private final PeopleDao peopleDao;
    private final AchievementStepProgressDao dao;

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
            final var step = stepsDao.read(stepId);
            verifyParent(achievementId.getUUID(), step);
            final var person = peopleDao.read(personId);
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
            final var step = stepsDao.read(stepId);
            verifyParent(achievementId.getUUID(), step);
            verifyCompletedProgress(dto);
            final var person = peopleDao.read(personId);
            final var properties =
                    dto.completed != null ?
                            new AchievementStepProgressProperties(dto.completed, dto.note) :
                            new AchievementStepProgressProperties(dto.value, dto.note);
            return map(dao.set(step, person, properties), ProgressDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    private void verifyCompletedProgress(ProgressDTO progress) {
        if (progress.completed == null && progress.value == null) {
            throw new BadRequestException("Either completed or progress must be set.");
        }
        if (progress.completed != null && progress.value != null) {
            if (AchievementStepProgressProperties.toProgress(progress.completed) != progress.value) {
                throw new BadRequestException("Properties completed and progress do not match.");
            }
        }
        if (progress.value != null) {
            if (progress.value < AchievementStepProgressProperties.PROGRESS_NOT_STARTED || progress.value > AchievementStepProgressProperties.PROGRESS_COMPLETED) {
                throw new BadRequestException("Progress must be between " + AchievementStepProgressProperties.PROGRESS_NOT_STARTED + " and " + AchievementStepProgressProperties.PROGRESS_COMPLETED);
            }
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
            final var step = stepsDao.read(stepId);
            verifyParent(achievementId.getUUID(), step);
            final var person = peopleDao.read(personId);
            dao.unset(step, person);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    private void verifyParent(UUID achievementId, AchievementStep step) throws ObjectNotFoundException {
        var achievement = achievementDao.read(achievementId);
        if (!step.getAchievement().getId().equals(achievement.getId())) {
            throw new NotFoundException();
        }
    }
}
