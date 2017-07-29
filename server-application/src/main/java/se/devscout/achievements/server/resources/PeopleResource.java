package se.devscout.achievements.server.resources;

import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("/organizations/{organizationId}/people")
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
    public List<PersonDTO> getByOrganization(@PathParam("organizationId") String organizationId) {
        getOrganization(organizationId);
        return dao.getByOrganization(organizationId).stream().map(p -> map(p, PersonDTO.class)).collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    @UnitOfWork
    public PersonDTO get(@PathParam("organizationId") String organizationId, @PathParam("id") String id) {
        try {
            final Person person = dao.get(id);
            verifyParent(organizationId, person);
            return map(person, PersonDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @POST
    @UnitOfWork
    public Response create(@PathParam("organizationId") String organizationId, PersonDTO input) {
        Organization organization = getOrganization(organizationId);
        final Person person = dao.create(organization, map(input, PersonProperties.class));
        final URI location = uriInfo.getRequestUriBuilder().path(person.getId().toString()).build();
        return Response
                .created(location)
                .entity(map(person, PersonDTO.class))
                .build();
    }

    private Organization getOrganization(@PathParam("organizationId") String organizationId) {
        Organization organization = null;
        try {
            organization = organizationsDao.get(organizationId);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
        return organization;
    }

    @DELETE
    @UnitOfWork
    @Path("{id}")
    public Response delete(@PathParam("organizationId") String organizationId, @PathParam("id") String id) {
        try {
            verifyParent(organizationId, dao.get(id));
            dao.delete(id);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private void verifyParent(String organizationId, Person person) throws ObjectNotFoundException {
        Organization organization = getOrganization(organizationId);
        if (!person.getOrganization().getId().equals(organization.getId())) {
            throw new NotFoundException();
        }
    }
}
