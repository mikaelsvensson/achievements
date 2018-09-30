package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.I18n;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.importer.CsvDataSource;
import se.devscout.achievements.server.data.importer.PeopleDataSource;
import se.devscout.achievements.server.data.importer.PeopleDataSourceException;
import se.devscout.achievements.server.data.importer.RepetDataSource;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.mail.Template;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("organizations/{organizationId}/people")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PeopleResource extends AbstractResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeopleResource.class);

    private PeopleDao dao;
    private OrganizationsDao organizationsDao;
    private AchievementsDao achievementsDao;
    private ObjectMapper objectMapper;
    private GroupsDao groupsDao;
    private GroupMembershipsDao membershipsDao;
    private PeopleDataSource[] peopleDataSources;
    private File tempDir;
    private final Template template;
    private URI guiApplicationHost;
    private EmailSender emailSender;
    private I18n i18n;

    public PeopleResource(PeopleDao dao, OrganizationsDao organizationsDao, AchievementsDao achievementsDao, ObjectMapper objectMapper, GroupsDao groupsDao, GroupMembershipsDao membershipsDao, URI guiApplicationHost, EmailSender emailSender, I18n i18n) {
        this.dao = dao;
        this.organizationsDao = organizationsDao;
        this.achievementsDao = achievementsDao;
        this.objectMapper = objectMapper;
        this.groupsDao = groupsDao;
        this.membershipsDao = membershipsDao;
        try {
            this.peopleDataSources = new PeopleDataSource[]{
                    new CsvDataSource(objectMapper),
                    new RepetDataSource(),
            };
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        this.tempDir = Files.createTempDir();
        this.guiApplicationHost = guiApplicationHost;
        this.template = new Template("assets/email.set-password.html");
        this.emailSender = emailSender;
        this.i18n = i18n;
    }

    @GET
    @UnitOfWork
    public List<PersonBaseDTO> getByOrganization(@PathParam("organizationId") UuidString organizationId,
                                                 @QueryParam("filter") String filter,
                                                 @QueryParam("group") String group,
                                                 @Auth User user) {
        final Organization organization = getOrganization(organizationId.getUUID());
        return dao.getByParent(organization).stream()
                .filter(person -> Strings.isNullOrEmpty(filter) || person.getName().toLowerCase().contains(filter.trim().toLowerCase()))
                .filter(person -> Strings.isNullOrEmpty(group) || person.getMemberships().stream().anyMatch(membership -> membership.getGroup().getId().equals(Integer.parseInt(group))))
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
            final Person person = getPerson(organizationId, id);
            final PersonDTO personDTO = map(person, PersonDTO.class);
            personDTO.organization = map(person.getOrganization(), OrganizationBaseDTO.class);
            return personDTO;
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private Person getPerson(UuidString organizationId, String id) throws ObjectNotFoundException {
        final Person person;
        if (id.startsWith("c:")) {
            final Organization organization = getOrganization(organizationId.getUUID());
            person = dao.read(organization, id.substring(2));
        } else {
            person = dao.read(Integer.parseInt(id));
            verifyParent(organizationId.getUUID(), person);
        }
        return person;
    }

    // TODO: Move to separate class
    @POST
    @Path("{personId}/mails/welcome")
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    public void postMail(@PathParam("organizationId") UuidString organizationId,
                         @PathParam("personId") String id,
                         @Auth User user,
                         @Context HttpServletRequest req) {
        try {
            final Person person = getPerson(organizationId, id);

            final String email = person.getEmail();
            final boolean isEmailSet = !Strings.isNullOrEmpty(StringUtils.trim(email));
            if (!isEmailSet) {
                // TODO: Provide better error message to user when e-mail is not set.
                throw new BadRequestException();
            }

            final URI loginLink = guiApplicationHost;
            final URI aboutLink = URI.create(StringUtils.appendIfMissing(guiApplicationHost.toString(), "/") + "#om");
            final boolean isGoogleAccount = isCredentialOfTypeSet(person, CredentialsType.GOOGLE);
            final boolean isMicrosoftAccount = isCredentialOfTypeSet(person, CredentialsType.MICROSOFT);

            final String body = new WelcomeUserTemplate().render(
                    aboutLink,
                    email,
                    isGoogleAccount,
                    isMicrosoftAccount,
                    // It is by definition an "e-mail account" since the person has an e-mail address.
                    true,
                    loginLink);

            emailSender.send(
                    req != null ? req.getRemoteAddr() : "ANONYMOUS",
                    email,
                    i18n.get("sendWelcomeMail.subject"),
                    body);
            // TODO: Save e-mail in database
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        } catch (EmailSenderException e) {
            throw new InternalServerErrorException();
        }
    }

    private boolean isCredentialOfTypeSet(Person person, CredentialsType type) {
        return person.getCredentials().stream().anyMatch(c -> c.getType() == type);
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

            final OrganizationAchievementSummaryDTO summary = createAchievementSummaryDTO(achievements, id, organizationId.getUUID());

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
        final UUID organizationUUID = organizationId.getUUID();
        List<UpsertPersonResultDTO> result = upsert(user, input, organizationUUID, false);
        return Response
                .ok(result.stream().map(r -> r.person).collect(Collectors.toList()))
                .build();
    }

    private List<UpsertPersonResultDTO> upsert(@Auth User user, List<PersonDTO> people, UUID organizationUUID, boolean isDryRun) {
        List<UpsertPersonResultDTO> result = new ArrayList<>();
        Organization organization = getOrganization(organizationUUID);
        for (PersonDTO dto : people) {
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

                    person = !isDryRun ? dao.update(person.getId(), newProperties) : map(newProperties, Person.class);
                    result.add(new UpsertPersonResultDTO(new PersonBaseDTO(person.getId(), person.getName()), false));
                } catch (ObjectNotFoundException e) {
                    person = !isDryRun ? dao.create(organization, newProperties) : map(newProperties, Person.class);
                    result.add(new UpsertPersonResultDTO(new PersonBaseDTO(person.getId(), person.getName()), true));
                }
                if (dto.groups != null && !isDryRun) {
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
        return result;
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

    @POST
    @RolesAllowed(Roles.EDITOR)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @UnitOfWork
    public Response upsertFromFormData(@PathParam("organizationId") UuidString organizationId,
                                       @Auth User user,
                                       @FormDataParam("importDryRun") boolean isDryRun,
                                       @FormDataParam("importRawData") FormDataBodyPart importRawData,
                                       @FormDataParam("importUploadedFileId") FormDataBodyPart importUploadedFileId,
                                       @FormDataParam("importFile") FormDataContentDisposition importFileDisposition,
                                       @FormDataParam("importFile") InputStream importFile) {
        File tempFile = null;
        UUID tempFileId = null;
        // TODO: Do we even need to save the uploaded file since the only client is a single-page application and thus quite easily can re-upload the file in case the user wants to do a dry-run before actually importing?
        if (importFile != null) {
            tempFileId = UUID.randomUUID();
            try {
                tempFile = getTempFile(tempFileId);
                FileUtils.copyInputStreamToFile(importFile, tempFile);
                LOGGER.info("Saved uploaded file as %s", tempFile.getAbsolutePath());
            } catch (Exception e) {
                throw new InternalServerErrorException("Could not cache uploaded file.", e);
            }
        }
        if (importUploadedFileId != null && !Strings.isNullOrEmpty(importUploadedFileId.getValue())) {
            try {
                tempFileId = UUID.fromString(importUploadedFileId.getValue());
                tempFile = getTempFile(tempFileId);
                if (!tempFile.isFile()) {
                    throw new BadRequestException("Referenced upload does not exist.");
                }
            } catch (Exception e) {
                throw new InternalServerErrorException("Could not cache uploaded file.", e);
            }
        }
        if (tempFile == null) {
            throw new BadRequestException("No data");
        }
        List<PersonDTO> people = null;
        for (PeopleDataSource dataSource : peopleDataSources) {
            // BOMInputStream required since Java normally does not support the BOM first in XML files exported from Repet
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(tempFile))))) {
                List<PersonDTO> tempValues = dataSource.read(reader);
                if (people == null || tempValues.size() > people.size()) {
                    people = tempValues;
                }
            } catch (PeopleDataSourceException e) {
                LOGGER.info(e.getMessage());
            } catch (IOException e) {
                throw new BadRequestException("Could not read data", e);
            }
        }
        if (people != null && !people.isEmpty()) {
            final UUID organizationUUID = organizationId.getUUID();
            List<UpsertPersonResultDTO> result = upsert(user, people, organizationUUID, isDryRun);
            return Response
                    .ok(new UpsertResultDTO(result, tempFileId.toString()))
                    .build();
        } else {
            throw new BadRequestException("Could not read data. No records found.");
        }
    }

    private File getTempFile(UUID uuid) {
        return new File(tempDir, "achievements-upload-temp-" + uuid.toString());
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
            List<PersonDTO> values = new CsvDataSource(objectMapper).read(new StringReader(input));
            return upsert(organizationId, user, values);
        } catch (PeopleDataSourceException e) {
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
