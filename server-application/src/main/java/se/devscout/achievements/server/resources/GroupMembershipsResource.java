package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.api.GroupMembershipDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Group;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("organizations/{organizationId}/groups/{groupId}/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupMembershipsResource extends AbstractResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupMembershipsResource.class);
    private final GroupsDao groupsDao;
    private final PeopleDao peopleDao;
    private final OrganizationsDao organizationsDao;
    private final GroupMembershipsDao dao;

    public GroupMembershipsResource(GroupsDao groupsDao, PeopleDao peopleDao, OrganizationsDao organizationsDao, GroupMembershipsDao dao) {
        this.groupsDao = groupsDao;
        this.peopleDao = peopleDao;
        this.organizationsDao = organizationsDao;
        this.dao = dao;
    }

    @GET
    @UnitOfWork
    public List<GroupMembershipDTO> get(@PathParam("organizationId") UuidString organizationId,
                                        @PathParam("groupId") Integer groupId,
                                        @Auth User user) {
        try {
            final var organization = organizationsDao.read(organizationId.getUUID());
            final var group = groupsDao.read(groupId);
            verifyParent(organization, null, group);
            return dao.getMemberships(group).stream().map(p -> map(p, GroupMembershipDTO.class)).collect(Collectors.toList());
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @POST
    @RolesAllowed(Roles.EDITOR)
    @Path("{personId}")
    @UnitOfWork
    public Response add(@PathParam("organizationId") UuidString organizationId,
                        @PathParam("groupId") Integer groupId,
                        @PathParam("personId") Integer personId,
                        GroupMembershipDTO input,
                        @Auth User user) {
        try {
            final var organization = organizationsDao.read(organizationId.getUUID());
            final var person = peopleDao.read(personId);
            final var group = groupsDao.read(groupId);
            verifyParent(organization, person, group);
            dao.add(person, group, input.role);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private void verifyParent(Organization organization, Person person, Group group) {
        if (person != null && organization.getId() != person.getOrganization().getId()) {
            LOGGER.warn("User/client tries to use a person from another organizations. Not cool.");
            throw new BadRequestException("Identifiers do not match.");
        }
        if (group != null && organization.getId() != group.getOrganization().getId()) {
            LOGGER.warn("User/client tries to use group from another organizations. Not cool.");
            throw new BadRequestException("Identifiers do not match.");
        }
    }

    @DELETE
    @RolesAllowed(Roles.EDITOR)
    @Path("{personId}")
    @UnitOfWork
    public Response delete(@PathParam("organizationId") UuidString organizationId,
                           @PathParam("groupId") Integer groupId,
                           @PathParam("personId") Integer personId,
                           GroupMembershipDTO input,
                           @Auth User user) {
        try {
            final var organization = organizationsDao.read(organizationId.getUUID());
            final var person = peopleDao.read(personId);
            final var group = groupsDao.read(groupId);
            verifyParent(organization, person, group);
            dao.remove(person, group);
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }
}
