package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.AchievementStepDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.AchievementStepsDao;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.DaoException;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProgressProperties;
import se.devscout.achievements.server.data.model.AchievementStepProperties;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("achievements/{achievementId}/steps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AchievementStepsResource extends AbstractResource {
    private AchievementStepsDao dao;
    private AchievementsDao achievementsDao;

    public AchievementStepsResource(AchievementStepsDao dao, AchievementsDao achievementsDao) {
        this.dao = dao;
        this.achievementsDao = achievementsDao;
    }

    @GET
    @UnitOfWork
    public List<AchievementStepDTO> getByAchievement(@PathParam("achievementId") UuidString achievementId) {
        final var achievement = getAchievement(achievementId.getUUID());
        return dao.getByParent(achievement).stream().map(p -> map(p, AchievementStepDTO.class)).collect(Collectors.toList());
    }

    @GET
    @Path("{stepId}")
    @UnitOfWork
    public AchievementStepDTO get(@PathParam("achievementId") UuidString achievementId,
                                  @PathParam("stepId") Integer id) {
        try {
            final var person = dao.read(id);
            verifyParent(achievementId.getUUID(), person);
            return map(person, AchievementStepDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @POST
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    public Response create(@PathParam("achievementId") UuidString achievementId,
                           @Auth User user,
                           AchievementStepDTO input) {
        try {
            final var properties = map(input, AchievementStepProperties.class);
            if (input.prerequisite_achievement != null) {
                properties.setPrerequisiteAchievement(achievementsDao.read(UuidString.toUUID(input.prerequisite_achievement)));
            }

            final var validator = Validation.buildDefaultValidatorFactory().getValidator();
            final var violations = validator.validate(properties);
            if (!violations.isEmpty()) {
                throw new BadRequestException(violations.stream().map(v -> v.getMessage()).collect(Collectors.joining()));
            }

            var achievement = getAchievement(achievementId.getUUID());
            final var person = dao.create(achievement, properties);
            final var location = uriInfo.getRequestUriBuilder().path(person.getId().toString()).build();
            return Response
                    .created(location)
                    .entity(map(person, AchievementStepDTO.class))
                    .build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        } catch (DaoException e) {
            throw new InternalServerErrorException();
        }
    }

    private Achievement getAchievement(UUID achievementId) {
        Achievement achievement = null;
        try {
            achievement = achievementsDao.read(achievementId);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
        return achievement;
    }

    @DELETE
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{stepId}")
    public Response delete(@PathParam("achievementId") UuidString achievementId,
                           @PathParam("stepId") Integer id,
                           @Auth User user) {
        try {
            final var step = dao.read(id);

            verifyParent(achievementId.getUUID(), step);

            verifyNotInProgress(step);

            dao.delete(id);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private void verifyNotInProgress(AchievementStep step) {
        final var isInProgressForOnePerson = step.getProgressList().stream().anyMatch(AchievementStepProgressProperties::isCompleted);
        if (isInProgressForOnePerson) {
            throw new ClientErrorException(Response.Status.CONFLICT);
        }
    }

    private void verifyParent(UUID achievementId, AchievementStep person) {
        var achievement = getAchievement(achievementId);
        if (!person.getAchievement().getId().equals(achievement.getId())) {
            throw new NotFoundException();
        }
    }
}
