package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.OrganizationAchievementSummaryDTO;
import se.devscout.achievements.server.api.OrganizationBaseDTO;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.OrganizationProperties;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrganizationsResource extends AbstractResource {
    private OrganizationsDao dao;
    private AchievementsDao achievementsDao;
    private PeopleDao peopleDao;
//    private AuthResourceUtil authResourceUtil;

    public OrganizationsResource(OrganizationsDao dao, AchievementsDao achievementsDao,/*, AuthResourceUtil authResourceUtil*/PeopleDao peopleDao) {
        this.dao = dao;
        this.achievementsDao = achievementsDao;
//        this.authResourceUtil = authResourceUtil;
        this.peopleDao = peopleDao;
    }

    @GET
    @Path("{organizationId}")
    @UnitOfWork
    public OrganizationDTO get(@PathParam("organizationId") UuidString id, @Auth User user) {
        try {
            final Organization organization = dao.read(id.getUUID());
            return map(organization, OrganizationDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("{organizationId}/basic")
    @UnitOfWork
    public OrganizationBaseDTO getBasic(@PathParam("organizationId") UuidString id) {
        try {
            final Organization organization = dao.read(id.getUUID());
            return map(organization, OrganizationBaseDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

/*
    @POST
    @Path("{organizationId}/signup")
    @UnitOfWork
    public Response existingOrganizationSignup(@PathParam("organizationId") UuidString id,
                                               SignupBaseDTO dto) {
        return authResourceUtil.existingOrganizationSignup(id, dto);
    }
*/

/*
    @POST
    @Path("signup")
    @UnitOfWork
    public Response newOrganizationSignup(SignupDTO dto) {
        return authResourceUtil.newOrganizationSignup(dto);
    }
*/

    @GET
    @Path("{organizationId}/achievement-summary")
    @UnitOfWork
    public OrganizationAchievementSummaryDTO getAchievementSummary(@PathParam("organizationId") UuidString id, @Auth User user) {
        try {
            final Organization organization = dao.read(id.getUUID());

            final List<Achievement> achievements = achievementsDao.findWithProgressForOrganization(organization);

            final OrganizationAchievementSummaryDTO summary = createAchievementSummaryDTO(achievements, null, organization, peopleDao);

            return summary;
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @UnitOfWork
    public List<OrganizationDTO> find(@QueryParam("filter") String filter, @Auth User user) {
        try {
            return dao.find(filter).stream().map(o -> map(o, OrganizationDTO.class)).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    public Response create(OrganizationDTO input,
                           @Auth User user) {
        try {
            final Organization organization = dao.create(map(input, OrganizationProperties.class));
            // TODO: Send welcome mail?
            final URI location = uriInfo.getRequestUriBuilder().path(UuidString.toString(organization.getId())).build();
            return Response
                    .created(location)
                    .entity(map(organization, OrganizationDTO.class))
                    .build();
        } catch (DaoException e) {
            return Response.serverError().build();
        }
    }

    @PUT
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{organizationId}")
    public Response update(@PathParam("organizationId") UuidString id,
                           OrganizationDTO input,
                           @Auth User user) {
        try {
            final Organization organization = dao.update(id.getUUID(), map(input, OrganizationProperties.class));
            return Response
                    .ok()
                    .entity(map(organization, OrganizationDTO.class))
                    .build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException("Could not find " + id.toString());
        } catch (DuplicateCustomIdentifier duplicateCustomIdentifier) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        } catch (DaoException e) {
            throw new InternalServerErrorException();
        }
    }

    @DELETE
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{organizationId}")
    public Response delete(@PathParam("organizationId") UuidString id,
                           @Auth User user) {
        try {
            dao.delete(id.getUUID());
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }
}
