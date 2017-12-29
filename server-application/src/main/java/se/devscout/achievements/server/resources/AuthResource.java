package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.StringUtils;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.api.SignupBaseDTO;
import se.devscout.achievements.server.api.SignupDTO;
import se.devscout.achievements.server.auth.*;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("")
public class AuthResource extends AbstractResource {
    private final JwtAuthenticator authenticator;
    private CredentialsDao credentialsDao;
    private PeopleDao peopleDao;
    private OrganizationsDao organizationsDao;
    //    private String googleClientId;
    private SecretValidatorFactory factory;

    public AuthResource(JwtAuthenticator authenticator, CredentialsDao credentialsDao, PeopleDao peopleDao, OrganizationsDao organizationsDao, SecretValidatorFactory factory) {
        this.authenticator = authenticator;
        this.credentialsDao = credentialsDao;
        this.peopleDao = peopleDao;
//        this.googleClientId = googleClientId;
        this.organizationsDao = organizationsDao;
        this.factory = factory;
    }

    @POST
    @Path("signin")
    @UnitOfWork
    public Response createToken(@Auth User user) {
        try {
            final Credentials credentials = credentialsDao.read(user.getCredentialsId());

            final Person person = credentials.getPerson();

            return generateTokenResponse(person);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private Response generateTokenResponse(Person person) {
        return Response
                .ok()
                .entity(new AuthTokenDTO(generateToken(person)))
                .build();
    }

    private String generateToken(Person person) {
        return authenticator.generateToken(
                person.getName(),
                person.getOrganization() != null ? person.getOrganization().getId() : null,
                person.getId());
    }

    @POST
    @Path("organizations/signup")
    @UnitOfWork
    public Response newOrganizationSignup(SignupDTO dto) {
        final List<Organization> organizations = organizationsDao.find(dto.new_organization_name);
        if (organizations.isEmpty()) {
            if (Strings.isNullOrEmpty(dto.new_organization_name)) {
                throw new BadRequestException("Name of new organization was not specified.");
            }

            try {
                // Create organization
                final Organization organization = organizationsDao.create(new OrganizationProperties(dto.new_organization_name));

                SecretValidator validator = getSecretValidator(dto);

                final SecretValidationResult validationResult = validator.validate(dto.identity_provider_data.toCharArray());

                // Create person
                final String email = StringUtils.defaultIfBlank(validationResult.getUserEmail(), dto.email);
                final String name = StringUtils.substringBefore(email, "@");
                final Person person = peopleDao.create(organization, new PersonProperties(name, email, Collections.emptySet(), null));

                createCredentials(person, validationResult.getUserName(), validator.getIdentityProvider(), validator.getSecret());

                return generateTokenResponse(person);
            } catch (DaoException e) {
                throw new InternalServerErrorException("Could not create organization '" + dto.new_organization_name + "'.");
            }
        } else {
            throw new ClientErrorException(Response.Status.CONFLICT);
        }
    }

    private SecretValidator getSecretValidator(SignupBaseDTO dto) {
        return factory.get(dto.identity_provider, dto.identity_provider_data);
    }

    private void createCredentials(Person person, String userName, IdentityProvider identityProvider, byte[] secret) throws DaoException {
        final CredentialsProperties credentialsProperties = new CredentialsProperties(userName, identityProvider, secret);
        credentialsDao.create(person, credentialsProperties);
    }

    @POST
    @Path("organizations/{organizationId}/signup")
    @UnitOfWork
    public Response existingOrganizationSignup(@PathParam("organizationId") UuidString id,
                                               SignupBaseDTO dto) {
        try {
            if (Strings.isNullOrEmpty(dto.email)) {
                throw new BadRequestException("Email cannot be empty");
            }
            try {
                final List<Person> people = peopleDao.getByEmail(dto.email);
                if (people.size() == 1) {
                    final Person person = people.get(0);

                    Organization organization = organizationsDao.read(id.getUUID());
                    if (person.getOrganization().getId() == organization.getId()) {

                        SecretValidator validator = getSecretValidator(dto);

                        final SecretValidationResult validationResult = validator.validate(dto.identity_provider_data.toCharArray());

                        createCredentials(person, validationResult.getUserName(), validator.getIdentityProvider(), validator.getSecret());

                        return generateTokenResponse(person);

//                    final URI location = UriBuilder.fromResource(PeopleResource.class)
//                            .path(person.getId().toString())
//                            .build(UuidString.toString(organization.getId()));
//                    return Response
//                            .created(location)
//                            .entity(new SignupResponseDTO(map(person, PersonDTO.class), map(organization, OrganizationDTO.class)))
//                            .build();
                    } else {
                        throw new ClientErrorException(dto.email + " is already registered with another organization.", Response.Status.BAD_REQUEST);
                    }
                } else if (people.isEmpty()) {
                    throw new ClientErrorException("Cannot find " + dto.email, Response.Status.BAD_REQUEST);
                } else {
                    throw new InternalServerErrorException("E-mail address cannot be used to sign up as multiple people share the same e-mail address.");
                }
            } catch (DaoException e) {
                throw new InternalServerErrorException("Could not configure user with credentials", e);
            }
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException("Could not find organization " + id);
        }
    }
}
