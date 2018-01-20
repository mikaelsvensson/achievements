package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("organizations/{organizationId}/people")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PeopleResource extends AbstractResource {
    private PeopleDao dao;
    private OrganizationsDao organizationsDao;
    private AchievementsDao achievementsDao;
    private ObjectMapper objectMapper;
    private GroupsDao groupsDao;
    private GroupMembershipsDao membershipsDao;

    public PeopleResource(PeopleDao dao, OrganizationsDao organizationsDao, AchievementsDao achievementsDao, ObjectMapper objectMapper, GroupsDao groupsDao, GroupMembershipsDao membershipsDao) {
        this.dao = dao;
        this.organizationsDao = organizationsDao;
        this.achievementsDao = achievementsDao;
        this.objectMapper = objectMapper;
        this.groupsDao = groupsDao;
        this.membershipsDao = membershipsDao;
    }

    @GET
    @UnitOfWork
    public List<PersonBaseDTO> getByOrganization(@PathParam("organizationId") UuidString organizationId,
                                                 @QueryParam("filter") String filter,
                                                 @Auth User user) {
        final Organization organization = getOrganization(organizationId.getUUID());
        return dao.getByParent(organization).stream()
                .filter(person -> Strings.isNullOrEmpty(filter) || person.getName().toLowerCase().contains(filter.trim().toLowerCase()))
                .map(p -> map(p, PersonBaseDTO.class))
                .collect(Collectors.toList());
    }

    @GET
    @Path("{personId}")
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    public PersonDTO get(@PathParam("organizationId") UuidString organizationId,
                         @PathParam("personId") String id,
                         @Auth User user) {
        try {
            final Person person;
            if (id.startsWith("c:")) {
                final Organization organization = getOrganization(organizationId.getUUID());
                person = dao.read(organization, id.substring(2));
            } else {
                person = dao.read(Integer.parseInt(id));
                verifyParent(organizationId.getUUID(), person);
            }
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
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    public Response create(@PathParam("organizationId") UuidString organizationId,
                           @Auth User user,
                           PersonDTO input) {
        try {
            checkRoleEscalation(input, user);
            Organization organization = getOrganization(organizationId.getUUID());
            final PersonProperties properties = map(input, PersonProperties.class);
            properties.setRole(Roles.READER);
            final Person person = dao.create(organization, properties);
            final URI location = uriInfo.getRequestUriBuilder().path(person.getId().toString()).build();
            return Response
                    .created(location)
                    .entity(map(person, PersonDTO.class))
                    .build();
        } catch (DuplicateCustomIdentifier e) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        } catch (DaoException e) {
            throw new InternalServerErrorException();
        }
    }

    @PUT
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    public Response upsert(@PathParam("organizationId") UuidString organizationId,
                           @Auth User user,
                           List<PersonDTO> input) {
        List<PersonBaseDTO> result = new ArrayList<>();
        Organization organization = getOrganization(organizationId.getUUID());
        for (PersonDTO dto : input) {
            try {
                Person person;
                final PersonProperties newProperties = map(dto, PersonProperties.class);
                try {
                    if (dto.id != null && dto.id > 0) {
                        person = dao.read(dto.id);
                    } else {
                        person = dao.read(organization, dto.custom_identifier);
                    }

                    checkSelfEditing(person.getId(), user);

                    person = dao.update(person.getId(), newProperties);
                    result.add(new PersonBaseDTO(person.getId(), person.getName()));
                } catch (ObjectNotFoundException e) {
                    person = dao.create(organization, newProperties);
                    result.add(new PersonBaseDTO(person.getId(), person.getName()));
                }
                if (dto.groups != null) {
                    for (GroupBaseDTO groupDto : dto.groups) {
                        assignGroup(organization, person, groupDto);
                    }
                }
            } catch (DuplicateCustomIdentifier e) {
                throw new WebApplicationException(Response.Status.CONFLICT);
            } catch (DaoException e) {
                throw new InternalServerErrorException();
            } catch (ObjectNotFoundException e) {
                throw new NotFoundException(e.getMessage());
            }
        }
        return Response
                .ok(result)
                .build();
    }

    private void assignGroup(Organization organization, Person person, GroupBaseDTO groupDto) throws ObjectNotFoundException, DaoException {
        Group group = null;
        if (groupDto.id != null) {
            group = groupsDao.read(groupDto.id);
        } else if (!Strings.isNullOrEmpty(groupDto.name)) {
            try {
                group = groupsDao.read(organization, groupDto.name);
            } catch (ObjectNotFoundException e) {
                group = groupsDao.create(organization, new GroupProperties(groupDto.name));
            }
        }
        if (group != null) {
            if (group.getOrganization().getId() != organization.getId()) {
                throw new BadRequestException("Bad identifiers");
            }
            membershipsDao.add(person, group, GroupRole.MEMBER);
        }
    }

    @PUT
    @RolesAllowed(Roles.EDITOR)
    @Consumes("text/csv")
    @UnitOfWork
    public Response upsertFromCsv(@PathParam("organizationId") UuidString organizationId,
                                  @Auth User user,
                                  String input) {
        LoggerFactory.getLogger(PeopleResource.class).info(input);

        try {
            CsvSchema schema = CsvSchema.emptySchema().withHeader(); // use first row as header; otherwise defaults are fine
            final List<Map<String, String>> values1 = Lists.newArrayList(
                    new CsvMapper().readerFor(Map.class)
                            .with(schema)
                            .readValues(input));

            List<PersonDTO> values = values1.stream().map(map -> {
                final String rawGroups = map.remove("groups");
                final PersonDTO dto = objectMapper.convertValue(map, PersonDTO.class);
                dto.attributes = new ArrayList<>();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (entry.getKey().startsWith("attr.")) {
                        dto.attributes.add(new PersonAttributeDTO(entry.getKey().substring("attr.".length()), entry.getValue()));
                    }
                }
                if (rawGroups != null) {
                    dto.groups = Stream.of(StringUtils.split(rawGroups, ',')).map(grp -> new GroupBaseDTO(null, grp.trim())).collect(Collectors.toList());
                }
                return dto;
            }).collect(Collectors.toList());
            return upsert(organizationId, user, values);
        } catch (IOException e) {
            throw new BadRequestException("Could not read data", e);
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
    @Path("{personId}")
    public Response update(@PathParam("organizationId") UuidString organizationId,
                           @PathParam("personId") Integer id,
                           PersonDTO input,
                           @Auth User user) {
        try {
            checkRoleEscalation(input, user);
            checkSelfEditing(id, user);
            final Person person = dao.update(id, map(input, PersonProperties.class));
            return Response
                    .ok()
                    .entity(map(person, PersonDTO.class))
                    .build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException("Could not find " + id.toString());
        } catch (DuplicateCustomIdentifier e) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        } catch (DaoException e) {
            throw new InternalServerErrorException();
        }
    }

    private void checkRoleEscalation(PersonDTO input, @Auth User user) {
        if (input.role != null && !user.getRoles().contains(input.role)) {
            throw new BadRequestException("You can only grant users your own roles.");
        }
    }

    private void checkSelfEditing(int requestPersonId, @Auth User user) {
        if (requestPersonId == user.getPersonId()) {
            throw new BadRequestException("You can not edit yourself.");
        }
    }

    @DELETE
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{personId}")
    public Response delete(@PathParam("organizationId") UuidString organizationId,
                           @PathParam("personId") Integer id,
                           @Auth User user) {
        try {
            checkSelfEditing(id, user);
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
