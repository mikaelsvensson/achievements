package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.auth.User;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("my")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MyResource extends AbstractResource {
    private PeopleDao peopleDao;
    private OrganizationsDao organizationsDao;

    public MyResource(PeopleDao peopleDao, OrganizationsDao organizationsDao) {
        this.peopleDao = peopleDao;
        this.organizationsDao = organizationsDao;
    }

    @GET
    @Path("people")
    @UnitOfWork
    public List<PersonDTO> get(@Auth User user) {
        return peopleDao.getByParent(user.getCredentials().getPerson().getOrganization()).stream()
                .map(p -> map(p, PersonDTO.class))
                .collect(Collectors.toList());
    }

}
