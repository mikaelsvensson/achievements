package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.OrganizationAchievementSummaryDTO;
import se.devscout.achievements.server.api.OrganizationBaseDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;
import se.devscout.achievements.server.auth.User;

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
    private AchievementsDao achievementsDao;

    public PeopleResource(PeopleDao dao, OrganizationsDao organizationsDao, AchievementsDao achievementsDao) {
        this.dao = dao;
        this.organizationsDao = organizationsDao;
        this.achievementsDao = achievementsDao;
    }

    @GET
    @UnitOfWork
    public List<PersonDTO> getByOrganization(@PathParam("organizationId") UuidString organizationId,
                                             @Auth User user) {
        final Organization organization = getOrganization(organizationId.getUUID());
        return dao.getByParent(organization).stream().map(p -> map(p, PersonDTO.class)).collect(Collectors.toList());
    }

    @GET
    @Path("{personId}")
    @UnitOfWork
    public PersonDTO get(@PathParam("organizationId") UuidString organizationId,
                         @PathParam("personId") Integer id,
                         @Auth User user) {
        try {
            final Person person = dao.read(id);
            verifyParent(organizationId.getUUID(), person);
            final PersonDTO personDTO = map(person, PersonDTO.class);
            personDTO.organization = map(person.getOrganization(), OrganizationBaseDTO.class);
            return personDTO;
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("{personId}/achievement-summary")
    @UnitOfWork
    public OrganizationAchievementSummaryDTO getAchievementSummary(@PathParam("organizationId") UuidString organizationId,
                                                                   @PathParam("personId") Integer id,
                                                                   @Auth User user) {
        try {
            final Person person = dao.read(id);
            verifyParent(organizationId.getUUID(), person);

            final List<Achievement> achievements = achievementsDao.findWithProgressForPerson(person);

            final OrganizationAchievementSummaryDTO summary = createAchievementSummaryDTO(achievements, id);

            return summary;
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }


    @POST
    @UnitOfWork
    public Response create(@PathParam("organizationId") UuidString organizationId,
                           @Auth User user,
                           PersonDTO input) {
        Organization organization = getOrganization(organizationId.getUUID());
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

    @PUT
    @UnitOfWork
    @Path("{personId}")
    public Response update(@PathParam("organizationId") UuidString organizationId,
                           @PathParam("personId") Integer id,
                           PersonDTO input,
                           @Auth User user) {
        try {
            final Person person = dao.update(id, map(input, PersonProperties.class));
            return Response
                    .ok()
                    .entity(map(person, PersonDTO.class))
                    .build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException("Could not find " + id.toString());
        }
    }

    @DELETE
    @UnitOfWork
    @Path("{personId}")
    public Response delete(@PathParam("organizationId") UuidString organizationId,
                           @PathParam("personId") Integer id,
                           @Auth User user) {
        try {
            verifyParent(organizationId.getUUID(), dao.read(id));
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
