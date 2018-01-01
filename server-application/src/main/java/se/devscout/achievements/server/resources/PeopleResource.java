package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Lists;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.api.OrganizationAchievementSummaryDTO;
import se.devscout.achievements.server.api.OrganizationBaseDTO;
import se.devscout.achievements.server.api.PersonBaseDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;
import se.devscout.achievements.server.resources.authenticator.User;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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
            Organization organization = getOrganization(organizationId.getUUID());
            final Person person = dao.create(organization, map(input, PersonProperties.class));
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
                    person = dao.update(person.getId(), newProperties);
                    result.add(new PersonBaseDTO(person.getId(), person.getName()));
                } catch (ObjectNotFoundException e) {
                    person = dao.create(organization, newProperties);
                    result.add(new PersonBaseDTO(person.getId(), person.getName()));
                }
            } catch (DuplicateCustomIdentifier e) {
                throw new WebApplicationException(Response.Status.CONFLICT);
            } catch (DaoException e) {
                throw new InternalServerErrorException();
            }
        }
        return Response
                .ok(result)
                .build();
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
            final List<PersonDTO> values = Lists.newArrayList(
                    new CsvMapper()
                            .readerFor(PersonDTO.class)
                            .with(CsvSchema.emptySchema().withHeader())
                            .readValues(input));
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

    @DELETE
    @RolesAllowed(Roles.EDITOR)
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
