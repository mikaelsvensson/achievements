package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.api.ProgressDTO;
import se.devscout.achievements.server.auth.User;
import se.devscout.achievements.server.data.dao.AchievementStepProgressDao;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.DaoException;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("achievements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AchievementsResource extends AbstractResource {
    private AchievementsDao dao;
    private AchievementStepProgressDao progressDao;

    public AchievementsResource(AchievementsDao dao, AchievementStepProgressDao progressDao) {
        this.dao = dao;
        this.progressDao = progressDao;
    }

    @GET
    @UnitOfWork
    @Path("{achievementId}/progress")
    public Map<String, ProgressDTO> getProgress(@PathParam("achievementId") UuidString id, @Auth User user) {
        try {
            final Achievement achievement = dao.read(id.getUUID());
            return progressDao
                    .get(achievement)
                    .stream()
                    .collect(Collectors.toMap(key -> key.getStep().getId() + "_" + key.getPerson().getId(), value -> map(value, ProgressDTO.class)));
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @UnitOfWork
    @Path("{achievementId}")
    public AchievementDTO get(@PathParam("achievementId") UuidString id) {
        try {
            return map(dao.read(id.getUUID()), AchievementDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @UnitOfWork
    public List<AchievementDTO> find(@QueryParam("filter") String filter) {
        try {
            return dao.find(filter).stream().map(o -> map(o, AchievementDTO.class)).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @UnitOfWork
    public Response create(AchievementDTO input, @Auth User user) {
        try {
            final Achievement achievement = dao.create(map(input, AchievementProperties.class));
            final URI location = uriInfo.getRequestUriBuilder().path(UuidString.toString(achievement.getId())).build();
            return Response
                    .created(location)
                    .entity(map(achievement, AchievementDTO.class))
                    .build();
        } catch (DaoException e) {
            return Response.serverError().build();
        }
    }

    @DELETE
    @UnitOfWork
    @Path("{achievementId}")
    public Response delete(@PathParam("achievementId") UuidString id, @Auth User user) {
        try {
            dao.delete(id.getUUID());
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }
}
