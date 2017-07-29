package se.devscout.achievements.server.resources;

import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.DaoException;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/achievements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AchievementsResource extends AbstractResource {
    private AchievementsDao dao;

    public AchievementsResource(AchievementsDao dao) {
        this.dao = dao;
    }

    @GET
    @Path("{id}")
    public AchievementDTO get(@PathParam("id") String id) {
        try {
            return map(dao.get(id), AchievementDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @POST
    @UnitOfWork
    public Response create(AchievementDTO input) {
        try {
            final Achievement achievement = dao.create(map(input, AchievementProperties.class));
            final URI location = uriInfo.getRequestUriBuilder().path(achievement.getId().toString()).build();
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
    @Path("{id}")
    public Response delete(@PathParam("id") String id) {
        try {
            dao.delete(id);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }
}
