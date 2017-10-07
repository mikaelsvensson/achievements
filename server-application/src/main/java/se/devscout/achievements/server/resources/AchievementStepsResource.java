package se.devscout.achievements.server.resources;

import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.AchievementStepDTO;
import se.devscout.achievements.server.data.dao.AchievementStepsDao;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProperties;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/achievements/{achievementId}/steps")
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
    public List<AchievementStepDTO> getByAchievement(@PathParam("achievementId") String achievementId) {
        final Achievement achievement = getAchievement(achievementId);
        return dao.getByParent(achievement).stream().map(p -> map(p, AchievementStepDTO.class)).collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    @UnitOfWork
    public AchievementStepDTO get(@PathParam("achievementId") String achievementId, @PathParam("id") String id) {
        try {
            final AchievementStep person = dao.get(id);
            verifyParent(achievementId, person);
            return map(person, AchievementStepDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @POST
    @UnitOfWork
    public Response create(@PathParam("achievementId") String achievementId, AchievementStepDTO input) throws ObjectNotFoundException {
        final AchievementStepProperties properties = map(input, AchievementStepProperties.class);
        if (input.prerequisite_achievement != null) {
            properties.setPrerequisiteAchievement(achievementsDao.get(input.prerequisite_achievement.toString()));
        }

        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final Set<ConstraintViolation<AchievementStepProperties>> violations = validator.validate(properties);
        if (!violations.isEmpty()) {
            throw new BadRequestException(violations.stream().map(v -> v.getMessage()).collect(Collectors.joining()));
        }

        Achievement achievement = getAchievement(achievementId);
        final AchievementStep person = dao.create(achievement, properties);
        final URI location = uriInfo.getRequestUriBuilder().path(person.getId().toString()).build();
        return Response
                .created(location)
                .entity(map(person, AchievementStepDTO.class))
                .build();
    }

    private Achievement getAchievement(@PathParam("achievementId") String achievementId) {
        Achievement achievement = null;
        try {
            achievement = achievementsDao.get(achievementId);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
        return achievement;
    }

    @DELETE
    @UnitOfWork
    @Path("{id}")
    public Response delete(@PathParam("achievementId") String achievementId, @PathParam("id") String id) {
        try {
            verifyParent(achievementId, dao.get(id));
            dao.delete(id);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private void verifyParent(String achievementId, AchievementStep person) throws ObjectNotFoundException {
        Achievement achievement = getAchievement(achievementId);
        if (!person.getAchievement().getId().equals(achievement.getId())) {
            throw new NotFoundException();
        }
    }
}