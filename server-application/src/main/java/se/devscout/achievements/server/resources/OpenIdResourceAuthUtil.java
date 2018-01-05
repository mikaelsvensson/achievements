package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.authenticator.JwtAuthenticator;
import se.devscout.achievements.server.resources.authenticator.User;

import javax.ws.rs.BadRequestException;
import java.util.Collections;
import java.util.List;

public class OpenIdResourceAuthUtil {
    private final JwtAuthenticator authenticator;
    private CredentialsDao credentialsDao;
    private PeopleDao peopleDao;
    private OrganizationsDao organizationsDao;

    public OpenIdResourceAuthUtil(JwtAuthenticator authenticator, CredentialsDao credentialsDao, PeopleDao peopleDao, OrganizationsDao organizationsDao) {
        this.authenticator = authenticator;
        this.credentialsDao = credentialsDao;
        this.peopleDao = peopleDao;
        this.organizationsDao = organizationsDao;
    }

    public AuthTokenDTO createToken(CredentialsType credentialsType, String userId) throws OpenIdResourceCallbackException {
        try {
            final Person person = credentialsDao.get(credentialsType, userId).getPerson();
            return generateTokenResponse(person);
        } catch (ObjectNotFoundException e) {
            throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.UNKNOWN_USER, "Could not find user.", e);
        }
    }

    public AuthTokenDTO createToken(User user) throws OpenIdResourceCallbackException {
        try {
            final Person person = credentialsDao.read(user.getCredentialsId()).getPerson();
            return generateTokenResponse(person);
        } catch (ObjectNotFoundException e) {
            throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.UNKNOWN_USER, "Could not find user.", e);
        }
    }

    private AuthTokenDTO generateTokenResponse(Person person) {
        return new AuthTokenDTO(generateToken(person));
    }

    private String generateToken(Person person) {
        return authenticator.generateToken(
                person.getName(),
                person.getOrganization() != null ? person.getOrganization().getId() : null,
                person.getId(),
                person.getRole());
    }

    public AuthTokenDTO newOrganizationSignup(String newOrganizationName,
                                              ValidationResult validationResult) throws OpenIdResourceCallbackException {

        if (Strings.isNullOrEmpty(newOrganizationName)) {
            throw new BadRequestException("Name of new organization was not specified.");
        }
        final List<Organization> organizations = organizationsDao.find(newOrganizationName);
        if (organizations.isEmpty()) {
            try {
                // Create organization
                final Organization organization = organizationsDao.create(new OrganizationProperties(newOrganizationName));

                // Create person
                final String email = validationResult.getUserEmail();
                final String name = StringUtils.substringBefore(email, "@");
                final Person person = peopleDao.create(organization, new PersonProperties(name, email, Collections.emptySet(), null, Roles.EDITOR));

                createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                return generateTokenResponse(person);
            } catch (DaoException e) {
                throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.SYSTEM_ERROR, "Could not create organization '" + newOrganizationName + "'.", e);
            }
        } else {
            throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.ORGANIZATION_EXISTS, "Organization " + newOrganizationName + " already exists.");
        }
    }

    private void createCredentials(Person person, String userName, CredentialsType credentialsType, byte[] secret) throws DaoException {
        final CredentialsProperties credentialsProperties = new CredentialsProperties(StringUtils.defaultString(userName, person.getEmail()), credentialsType, secret);
        credentialsDao.create(person, credentialsProperties);
    }

    public AuthTokenDTO existingOrganizationSignup(UuidString id,
                                                   ValidationResult validationResult) throws OpenIdResourceCallbackException {
        try {
            if (Strings.isNullOrEmpty(validationResult.getUserEmail())) {
                throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.INVALID_INPUT, "E-mail cannot be empty");
            }
            try {
                final List<Person> people = peopleDao.getByEmail(validationResult.getUserEmail());
                if (people.size() == 1) {
                    final Person person = people.get(0);

                    Organization organization = organizationsDao.read(id.getUUID());
                    if (person.getOrganization().getId() == organization.getId()) {

                        createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                        return generateTokenResponse(person);
                    } else {
                        throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.UNKNOWN_USER, validationResult.getUserEmail() + " is already registered with another organization.");
                    }
                } else if (people.isEmpty()) {
                    throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.INVALID_INPUT, "Cannot find " + validationResult.getUserEmail());
                } else {
                    throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.SYSTEM_ERROR, "E-mail address cannot be used to sign up as multiple people share the same e-mail address.");
                }
            } catch (DaoException e) {
                throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.SYSTEM_ERROR, "Could not configure user with credentials", e);
            }
        } catch (ObjectNotFoundException e) {
            throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.UNKNOWN_ORGANIZATION, "Could not find organization " + id, e);
        }
    }

    public AuthTokenDTO newSignup(ValidationResult validationResult) throws OpenIdResourceCallbackException {
        if (Strings.isNullOrEmpty(validationResult.getUserEmail())) {
            throw new BadRequestException("Email cannot be empty");
        }
        try {
            final List<Person> people = peopleDao.getByEmail(validationResult.getUserEmail());
            if (people.size() == 1) {
                final Person person = people.get(0);

                createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                return generateTokenResponse(person);
            } else if (people.isEmpty()) {
                throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.UNKNOWN_USER, "Cannot find " + validationResult.getUserEmail());
            } else {
                throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.SYSTEM_ERROR, "E-mail address cannot be used to sign up as multiple people share the same e-mail address.");
            }
        } catch (DaoException e) {
            throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.SYSTEM_ERROR, "Could not configure user with credentials", e);
        }
    }
}
