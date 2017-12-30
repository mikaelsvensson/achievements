package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.api.SignupBaseDTO;
import se.devscout.achievements.server.api.SignupDTO;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.CredentialsValidatorFactory;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.authenticator.JwtAuthenticator;
import se.devscout.achievements.server.resources.authenticator.User;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

public class AuthResourceUtil {
    private final JwtAuthenticator authenticator;
    private CredentialsDao credentialsDao;
    private PeopleDao peopleDao;
    private OrganizationsDao organizationsDao;
    private CredentialsValidatorFactory factory;

    public AuthResourceUtil(JwtAuthenticator authenticator, CredentialsDao credentialsDao, PeopleDao peopleDao, OrganizationsDao organizationsDao, CredentialsValidatorFactory factory) {
        this.authenticator = authenticator;
        this.credentialsDao = credentialsDao;
        this.peopleDao = peopleDao;
        this.organizationsDao = organizationsDao;
        this.factory = factory;
    }

    public Response createToken(User user) {
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

    public Response newOrganizationSignup(SignupDTO dto) {
        if (Strings.isNullOrEmpty(dto.new_organization_name)) {
            throw new BadRequestException("Name of new organization was not specified.");
        }
        final List<Organization> organizations = organizationsDao.find(dto.new_organization_name);
        if (organizations.isEmpty()) {

            try {
                // Create organization
                final Organization organization = organizationsDao.create(new OrganizationProperties(dto.new_organization_name));

                CredentialsValidator validator = getCredentialsValidator(dto);

                final ValidationResult validationResult = validator.validate(dto.credentials_data.toCharArray());

                // Create person
                final String email = StringUtils.defaultIfBlank(validationResult.getUserEmail(), dto.email);
                final String name = StringUtils.substringBefore(email, "@");
                final Person person = peopleDao.create(organization, new PersonProperties(name, email, Collections.emptySet(), null));

                createCredentials(person, validationResult.getUserId(), validator.getCredentialsType(), validator.getCredentialsData());

                return generateTokenResponse(person);
            } catch (DaoException e) {
                throw new InternalServerErrorException("Could not create organization '" + dto.new_organization_name + "'.");
            }
        } else {
            throw new ClientErrorException(Response.Status.CONFLICT);
        }
    }

    private CredentialsValidator getCredentialsValidator(SignupBaseDTO dto) {
        return factory.get(dto.credentials_type, dto.credentials_data);
    }

    private void createCredentials(Person person, String userName, CredentialsType credentialsType, byte[] secret) throws DaoException {
        final CredentialsProperties credentialsProperties = new CredentialsProperties(StringUtils.defaultString(userName, person.getEmail()), credentialsType, secret);
        credentialsDao.create(person, credentialsProperties);
    }

    public Response existingOrganizationSignup(UuidString id,
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

                        CredentialsValidator validator = getCredentialsValidator(dto);

                        final ValidationResult validationResult = validator.validate(dto.credentials_data.toCharArray());

                        createCredentials(person, validationResult.getUserId(), validator.getCredentialsType(), validator.getCredentialsData());

                        return generateTokenResponse(person);
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
