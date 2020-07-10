package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.I18n;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.importer.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.mail.template.WelcomeUserTemplate;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
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

    private final PeopleDao dao;
    private final OrganizationsDao organizationsDao;
    private final AchievementsDao achievementsDao;
    private final ObjectMapper objectMapper;
    private final GroupsDao groupsDao;
    private final GroupMembershipsDao membershipsDao;
    private PeopleDataSource[] peopleDataSources;
    private File tempDir;
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
                    new ScoutnetDataSource(objectMapper),
                    new RepetDataSource(),
            };
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        this.tempDir = Files.createTempDir();
        this.guiApplicationHost = guiApplicationHost;
        this.emailSender = emailSender;
        this.i18n = i18n;
    }

    @GET
    @UnitOfWork
    public List<PersonBaseDTO> getByOrganization(@PathParam("organizationId") UuidString organizationId,
                                                 @QueryParam("filter") String filter,
                                                 @QueryParam("group") String group,
                                                 @Auth User user) {
        final var organization = getOrganization(organizationId.getUUID());
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
            final var person = getPerson(organizationId, id);
            final var personDTO = map(person, PersonDTO.class);
            personDTO.organization = map(person.getOrganization(), OrganizationBaseDTO.class);
            return personDTO;
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private Person getPerson(UuidString organizationId, String id) throws ObjectNotFoundException {
        final Person person;
        if (id.startsWith("c:")) {
            final var organization = getOrganization(organizationId.getUUID());
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
            final var person = getPerson(organizationId, id);

            final var email = person.getEmail();
            final var isEmailSet = !Strings.isNullOrEmpty(StringUtils.trim(email));
            if (!isEmailSet) {
                // TODO: Provide better error message to user when e-mail is not set.
                throw new BadRequestException();
            }

            final var loginLink = guiApplicationHost;
            final var aboutLink = URI.create(StringUtils.appendIfMissing(guiApplicationHost.toString(), "/") + "#om");

            final var isGoogleAccount = isProvidedBy(email, "gmail.com")
                    || isCredentialOfTypeSet(person, CredentialsType.GOOGLE);

            final var isMicrosoftAccount = !isGoogleAccount &&
                    (isProvidedBy(email, "outlook.com", "hotmail.com")
                            || isCredentialOfTypeSet(person, CredentialsType.MICROSOFT));

            final var isEmailAccount = !isGoogleAccount
                    && !isMicrosoftAccount
                    && isEmailSet;

            final var body = new WelcomeUserTemplate().render(
                    aboutLink,
                    email,
                    isGoogleAccount,
                    isMicrosoftAccount,
                    isEmailAccount,
                    loginLink);

            emailSender.send(
                    req != null ? req.getRemoteAddr() : "ANONYMOUS",
                    email,
                    i18n.get("sendWelcomeMail.subject"),
                    body);
            // TODO: Save e-mail in database
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        } catch (EmailSenderException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private boolean isProvidedBy(String email, String... hosts) {
        try {
            // TODO: Move parsing (attempt) to data model validation instead.
            final var addresses = InternetAddress.parse(email);
            if (addresses.length != 1) {
                throw new InternalServerErrorException("Invalid e-mail address.");
            }
            var address = addresses[0];
            for (var host : hosts) {
                if (address.getAddress().endsWith(host)) {
                    return true;
                }
            }
            return false;
        } catch (AddressException e) {
            throw new InternalServerErrorException("Invalid e-mail address.");
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
            final var person = dao.read(id);
            verifyParent(organizationId.getUUID(), person);

            final var achievements = achievementsDao.findWithProgressForPerson(person);

            final var summary = createAchievementSummaryDTO(achievements, person, person.getOrganization(), dao);

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
            var organization = getOrganization(organizationId.getUUID());
            final var properties = map(input, PersonProperties.class);
            properties.setRole(Roles.READER);
            final var person = dao.create(organization, properties);
            final var location = uriInfo.getRequestUriBuilder().path(person.getId().toString()).build();
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
        final var organizationUUID = organizationId.getUUID();
        var result = upsert(user, input, organizationUUID, false, false);
        return Response
                .ok(result.stream().map(r -> r.person).collect(Collectors.toList()))
                .build();
    }

    private List<UpsertPersonResultDTO> upsert(@Auth User user, List<PersonDTO> people, UUID organizationUUID, boolean isDryRun, boolean clearGroups) {
        List<UpsertPersonResultDTO> result = new ArrayList<>();
        var organization = getOrganization(organizationUUID);
        for (var dto : people) {
            checkRoleEscalation(dto, user);
            try {
                Person person;
                final var newProperties = map(dto, PersonProperties.class);
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
                    for (var groupDto : dto.groups) {
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
        if (clearGroups) {
            clearGroupsOfNonImportedPeople(organization, people);
        }
        return result;
    }

    private void clearGroupsOfNonImportedPeople(Organization organization, List<PersonDTO> people) {
        final var groupNames = people.stream().flatMap(p -> p.groups.stream()).map(g -> g.name).collect(Collectors.toSet());
        for (var groupName : groupNames) {
            try {
                final var group = groupsDao.read(organization, groupName);
                final var groupMemberships = membershipsDao.getMemberships(group);
                for (var groupMembership : groupMemberships) {
                    var shouldBeMember = people.stream()
                            // Look for people in the import matching the group membership in question (should result in zero or one match):
                            .filter(p -> StringUtils.equalsIgnoreCase(p.custom_identifier, groupMembership.getPerson().getCustomIdentifier())
                                    || StringUtils.equalsIgnoreCase(p.name, groupMembership.getPerson().getName()))

                            // Get the names of the groups that this person should be a member of:
                            .flatMap(p -> p.groups.stream()).map(g -> g.name)

                            // Check if any of the groups match the group membership in question:
                            .anyMatch(s -> StringUtils.equalsIgnoreCase(s, groupName));
                    if (!shouldBeMember) {
                        membershipsDao.remove(groupMembership.getPerson(), groupMembership.getGroup());
                    }
                }
            } catch (ObjectNotFoundException e) {
                // TODO: Handle exception?
            }
        }
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
                                       @FormDataParam("importClearGroups") boolean clearGroups,
                                       @FormDataParam("importRawData") FormDataBodyPart importRawData,
                                       @FormDataParam("importUploadedFileId") FormDataBodyPart importUploadedFileId,
                                       @FormDataParam("importFile") InputStream importFile) {

        var tempFileId = getUploadedDataFileId(importRawData, importUploadedFileId, importFile);

        List<PersonDTO> people = null;
        for (var dataSource : peopleDataSources) {
            // BOMInputStream required since Java normally does not support the BOM first in XML files exported from Repet
            try (var reader = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(getTempFile(tempFileId)))))) {
                var tempValues = dataSource.read(reader);
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
            final var organizationUUID = organizationId.getUUID();
            var result = upsert(user, people, organizationUUID, isDryRun, clearGroups);
            return Response
                    .ok(new UpsertResultDTO(result, tempFileId.toString()))
                    .build();
        } else {
            throw new BadRequestException("Could not read data. No records found.");
        }
    }

    private UUID getUploadedDataFileId(FormDataBodyPart importRawData, FormDataBodyPart importUploadedFileId, InputStream importFile) {
        UUID tempFileId;
        // TODO: Do we even need to save the uploaded file since the only client is a single-page application and thus quite easily can re-upload the file in case the user wants to do a dry-run before actually importing?
        if (importUploadedFileId != null && !Strings.isNullOrEmpty(importUploadedFileId.getValue())) {
            try {
                tempFileId = UUID.fromString(importUploadedFileId.getValue());
                if (!getTempFile(tempFileId).isFile()) {
                    throw new BadRequestException("Referenced upload does not exist.");
                }
            } catch (Exception e) {
                throw new InternalServerErrorException("Could not cache uploaded file.", e);
            }
        } else if (importFile != null) {
            tempFileId = UUID.randomUUID();
            try {
                FileUtils.copyInputStreamToFile(importFile, getTempFile(tempFileId));
                LOGGER.info("Saved uploaded file as {}", getTempFile(tempFileId).getAbsolutePath());
            } catch (Exception e) {
                throw new InternalServerErrorException("Could not cache uploaded file.", e);
            }
        } else if (!Strings.isNullOrEmpty(importRawData.getValue())) {
            tempFileId = UUID.randomUUID();
            try {
                Files.write(importRawData.getValue(), getTempFile(tempFileId), Charsets.UTF_8);
            } catch (IOException e) {
                throw new InternalServerErrorException("Could not cache uploaded data.", e);
            }
        } else {
            throw new BadRequestException("No data");
        }
        return tempFileId;
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
            var values = new CsvDataSource(objectMapper).read(new StringReader(input));
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
            final var person = dao.update(id, map(input, PersonProperties.class));
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

    private void verifyParent(UUID organizationId, Person person) {
        var organization = getOrganization(organizationId);
        if (!person.getOrganization().getId().equals(organization.getId())) {
            throw new NotFoundException();
        }
    }

}
