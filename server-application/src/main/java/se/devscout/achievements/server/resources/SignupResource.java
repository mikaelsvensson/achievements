package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@Path("signup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SignupResource extends AbstractResource {
    private PeopleDao peopleDao;
    private OrganizationsDao organizationsDao;
    private CredentialsDao credentialsDao;

    public SignupResource(PeopleDao peopleDao, OrganizationsDao organizationsDao, CredentialsDao credentialsDao) {
        this.peopleDao = peopleDao;
        this.organizationsDao = organizationsDao;
        this.credentialsDao = credentialsDao;
    }

    @POST
    @UnitOfWork
    public Response signupWithoutOrganization(SignupDTO dto) {
        if (Strings.isNullOrEmpty(dto.new_organization_name)) {
            throw new BadRequestException("Name of new organization was not specified.");
        }
        try {
            final List<Organization> existingOrganization = organizationsDao.find(dto.new_organization_name);
            if (existingOrganization == null || existingOrganization.isEmpty()) {
                final Organization organization = organizationsDao.create(new OrganizationProperties(dto.new_organization_name));
                return signup(dto, organization);
            } else {
                throw new ClientErrorException(Response.Status.CONFLICT);
            }
        } catch (DaoException e) {
            throw new ServerErrorException("Could not create organization '" + dto.new_organization_name + "'.", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @UnitOfWork
    @Path("{organizationId}")
    public Response signupForOrganization(@PathParam("organizationId") UuidString id, SignupBaseDTO dto) {
        try {
            Organization organization = organizationsDao.read(id.getUUID());
            return signup(dto, organization);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException("Could not find organization " + id);
        }
    }

    private Response signup(SignupBaseDTO dto, Organization organization) {
        if (Strings.isNullOrEmpty(dto.password)) {
            throw new BadRequestException("Password cannot be empty");
        }
        if (Strings.isNullOrEmpty(dto.name)) {
            throw new BadRequestException("Name cannot be empty");
        }
        try {
            final Person person = peopleDao.create(organization, new PersonProperties(dto.name, dto.email, Collections.emptySet()));
            credentialsDao.create(person, new CredentialsProperties(dto.email, new PasswordValidator(SecretGenerator.PDKDF2, dto.password.toCharArray())));
            final URI location = UriBuilder.fromResource(PeopleResource.class)
                    .path(person.getId().toString())
                    .build(UuidString.toString(organization.getId()));
            return Response
                    .created(location)
                    .entity(new SignupResponseDTO(map(person, PersonDTO.class), map(organization, OrganizationDTO.class)))
                    .build();
        } catch (IOException e) {
            throw new InternalServerErrorException("Could not configure user with credentials", e);
        }
    }
}
