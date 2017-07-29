package se.devscout.achievements.server.resources;

import se.devscout.achievements.server.data.dao.OrganizationsDao;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StatsResource extends AbstractResource {
    private OrganizationsDao dao;

    public StatsResource(OrganizationsDao dao) {
        this.dao = dao;
    }

    @GET
    public Response get() {
        Map<String, String> properties = new HashMap<>();
        properties.put("organisation_count", "123");
        return Response.ok().entity(properties).build();
    }

}
