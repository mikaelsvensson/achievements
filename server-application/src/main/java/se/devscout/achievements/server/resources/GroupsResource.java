package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.GroupDTO;
import se.devscout.achievements.server.api.OrganizationBaseDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Group;
import se.devscout.achievements.server.data.model.GroupProperties;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("organizations/{organizationId}/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupsResource extends AbstractResource {
    private final GroupsDao dao;
    private final OrganizationsDao organizationsDao;
    private final ObjectMapper objectMapper;

    public GroupsResource(GroupsDao dao, OrganizationsDao organizationsDao, AchievementsDao achievementsDao, ObjectMapper objectMapper) {
        this.dao = dao;
        this.organizationsDao = organizationsDao;
        this.objectMapper = objectMapper;
    }

    @GET
    @UnitOfWork
    public List<GroupDTO> getByOrganization(@PathParam("organizationId") UuidString organizationId,
                                            @Auth User user) {
        final var organization = getOrganization(organizationId.getUUID());
        return dao.getByParent(organization).stream().map(p -> map(p, GroupDTO.class)).collect(Collectors.toList());
    }

    @GET
    @Path("{groupId}")
    @UnitOfWork
    public GroupDTO get(@PathParam("organizationId") UuidString organizationId,
                        @PathParam("groupId") Integer id,
                        @Auth User user) {
        try {
            final var group = dao.read(id);
            verifyParent(organizationId.getUUID(), group);
            final var groupDTO = map(group, GroupDTO.class);
//            groupDTO.people = group.getMembers().stream().map(membership -> map(membership.getPerson(), PersonBaseDTO.class)).collect(Collectors.toList());
            groupDTO.organization = map(group.getOrganization(), OrganizationBaseDTO.class);
            return groupDTO;
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @POST
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    public Response create(@PathParam("organizationId") UuidString organizationId,
                           @Auth User user,
                           GroupDTO input) {
        try {
            var organization = getOrganization(organizationId.getUUID());
            final var properties = map(input, GroupProperties.class);
            final var group = dao.create(organization, properties);
            final var location = uriInfo.getRequestUriBuilder().path(group.getId().toString()).build();
            return Response
                    .created(location)
                    .entity(map(group, GroupDTO.class))
                    .build();
        } catch (DuplicateCustomIdentifier e) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        } catch (DaoException e) {
            throw new InternalServerErrorException();
        }
    }

    private Organization getOrganization(UUID organizationId) {
        Organization organization = null;
        try {
            organization = organizationsDao.read(organizationId);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
        return organization;
    }

    @PUT
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{groupId}")
    public Response update(@PathParam("organizationId") UuidString organizationId,
                           @PathParam("groupId") Integer id,
                           GroupDTO input,
                           @Auth User user) {
        try {
            final var group = dao.update(id, map(input, GroupProperties.class));
            return Response
                    .ok()
                    .entity(map(group, GroupDTO.class))
                    .build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException("Could not find " + id.toString());
        } catch (DuplicateCustomIdentifier e) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        } catch (DaoException e) {
            throw new InternalServerErrorException();
        }
    }

    @DELETE
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{groupId}")
    public Response delete(@PathParam("organizationId") UuidString organizationId,
                           @PathParam("groupId") Integer id,
                           @Auth User user) {
        try {
            verifyParent(organizationId.getUUID(), dao.read(id));
            dao.delete(id);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private void verifyParent(UUID organizationId, Group group) {
        var organization = getOrganization(organizationId);
        if (!group.getOrganization().getId().equals(organization.getId())) {
            throw new NotFoundException();
        }
    }
}
