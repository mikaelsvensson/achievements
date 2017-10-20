package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.api.SignupDTO;
import se.devscout.achievements.server.api.SignupResponseDTO;
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
    public Response signup(SignupDTO dto) {
        if (Strings.isNullOrEmpty(dto.user_password)) {
            throw new BadRequestException("Password cannot be empty");
        }
        if (Strings.isNullOrEmpty(dto.person_name)) {
            throw new BadRequestException("Name cannot be empty");
        }
        final Organization organization;
        if (dto.existing_organization_id != null) {
            try {
                organization = organizationsDao.read(dto.existing_organization_id);
            } catch (ObjectNotFoundException e) {
                throw new NotFoundException("Could not find organization " + dto.existing_organization_id);
            }
        } else if (!Strings.isNullOrEmpty(dto.new_organization_name)) {
            try {
                final List<Organization> existingOrganization = organizationsDao.find(dto.new_organization_name);
                if (existingOrganization == null || existingOrganization.isEmpty()) {
                    organization = organizationsDao.create(new OrganizationProperties(dto.new_organization_name));
                } else {
                    throw new ClientErrorException(Response.Status.CONFLICT);
                }
            } catch (DaoException e) {
                throw new ServerErrorException("Could not create organization '" + dto.new_organization_name + "'.", Response.Status.INTERNAL_SERVER_ERROR);
            }
        } else {
            throw new BadRequestException("Neither id of existing organization or name of new organization was specified.");
        }
        try {
            final Person person = peopleDao.create(organization, new PersonProperties(dto.person_name));
            credentialsDao.create(person, new CredentialsProperties(dto.person_name, new PasswordValidator(SecretGenerator.PDKDF2, dto.user_password.toCharArray())));
            final URI location = UriBuilder.fromResource(PeopleResource.class).path(person.getId().toString()).build(organization.getId().toString());
            return Response
                    .created(location)
                    .entity(new SignupResponseDTO(map(person, PersonDTO.class), map(organization, OrganizationDTO.class)))
                    .build();
        } catch (IOException e) {
            throw new InternalServerErrorException("Could not configure user with credentials", e);
        }
    }
}
