package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.uti.User;

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
        //FIXME: This currently returns ALL people in the system, since there is no concept of users at the moment
        return organizationsDao.all()
                .stream()
                .flatMap(organization -> peopleDao.getByParent(organization).stream())
                .map(p -> map(p, PersonDTO.class))
                .collect(Collectors.toList());
    }

}
