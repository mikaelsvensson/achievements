package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;
import se.devscout.achievements.server.uti.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("organizations/{organizationId}/people")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PeopleResource extends AbstractResource {
    private PeopleDao dao;
    private OrganizationsDao organizationsDao;

    public PeopleResource(PeopleDao dao, OrganizationsDao organizationsDao) {
        this.dao = dao;
        this.organizationsDao = organizationsDao;
    }

    @GET
    @UnitOfWork
    public List<PersonDTO> getByOrganization(@PathParam("organizationId") UUID organizationId,
                                             @Auth User user) {
        final Organization organization = getOrganization(organizationId);
        return dao.getByParent(organization).stream().map(p -> map(p, PersonDTO.class)).collect(Collectors.toList());
    }

    @GET
    @Path("{personId}")
    @UnitOfWork
    public PersonDTO get(@PathParam("organizationId") UUID organizationId,
                         @PathParam("personId") Integer id,
                         @Auth User user) {
        try {
            final Person person = dao.read(id);
            verifyParent(organizationId, person);
            return map(person, PersonDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @POST
    @UnitOfWork
    public Response create(@PathParam("organizationId") UUID organizationId,
                           @Auth User user,
                           PersonDTO input) {
        Organization organization = getOrganization(organizationId);
        final Person person = dao.create(organization, map(input, PersonProperties.class));
        final URI location = uriInfo.getRequestUriBuilder().path(person.getId().toString()).build();
        return Response
                .created(location)
                .entity(map(person, PersonDTO.class))
                .build();
    }

    private Organization getOrganization( UUID organizationId) {
        Organization organization = null;
        try {
            organization = organizationsDao.read(organizationId);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
        return organization;
    }

    @DELETE
    @UnitOfWork
    @Path("{personId}")
    public Response delete(@PathParam("organizationId") UUID organizationId,
                           @PathParam("personId") Integer id,
                           @Auth User user) {
        try {
            verifyParent(organizationId, dao.read(id));
            dao.delete(id);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private void verifyParent(UUID organizationId, Person person) throws ObjectNotFoundException {
        Organization organization = getOrganization(organizationId);
        if (!person.getOrganization().getId().equals(organization.getId())) {
            throw new NotFoundException();
        }
    }
}
