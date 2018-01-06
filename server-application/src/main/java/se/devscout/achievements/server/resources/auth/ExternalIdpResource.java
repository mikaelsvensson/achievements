package se.devscout.achievements.server.resources.auth;

import com.google.common.base.Strings;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.StringUtils;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.*;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.UuidString;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//TODO: Test this resource!
//TODO: Rename resource since the "e-mail provider" is not an OpenID IdP.
@Path("openid/{identityProvider}")
public class ExternalIdpResource extends AbstractAuthResource {

    private final Map<String, IdentityProvider> identityProviders;
    //TODO: Two utility classes for dealing with tokens seems a bit much, doesn't it?
    private JwtSignUpTokenService callbackStateTokenService;
    private CredentialsDao credentialsDao;
    private PeopleDao peopleDao;
    private OrganizationsDao organizationsDao;

    public ExternalIdpResource(JwtTokenService tokenService,
                               Map<String, IdentityProvider> identityProviders,
                               CredentialsDao credentialsDao,
                               PeopleDao peopleDao,
                               OrganizationsDao organizationsDao) {
        super(new JwtSignInTokenService(tokenService), credentialsDao);
        this.identityProviders = identityProviders;
        this.callbackStateTokenService = new JwtSignUpTokenService(tokenService);
        this.credentialsDao = credentialsDao;
        this.peopleDao = peopleDao;
        this.organizationsDao = organizationsDao;
    }

    @GET
    @Path("signin")
    @UnitOfWork
    public Response doSignInRequest(@PathParam("identityProvider") String identityProvider,
                                    @QueryParam("email") String email) throws ExternalIdpCallbackException {
        try {
            IdentityProvider idp = getIdentityProvider(identityProvider);
            final String state = callbackStateTokenService.encode(new JwtSignUpToken(email));
            return Response.temporaryRedirect(idp.getRedirectUri(state, getCallbackUri(identityProvider, "signin/callback"))).build();
        } catch (Exception e) {
            throw new ExternalIdpCallbackException(e);
        }
    }

    @GET
    @Path("signup")
    @UnitOfWork
    public Response doSignUpRequest(@PathParam("identityProvider") String identityProvider,
                                    @QueryParam("organization_id") UuidString organizationId,
                                    @QueryParam("new_organization_name") String organizationName,
                                    @QueryParam("email") String email) throws ExternalIdpCallbackException {
        try {
            IdentityProvider idp = getIdentityProvider(identityProvider);
            final String state = callbackStateTokenService.encode(new JwtSignUpToken(email, organizationId, organizationName));
            return Response.temporaryRedirect(idp.getRedirectUri(state, getCallbackUri(identityProvider, "signup/callback"))).build();
        } catch (Exception e) {
            throw new ExternalIdpCallbackException(e);
        }
    }


    @GET
    @Path("signin/callback")
    @UnitOfWork
    public Response handleSignInCallback(@PathParam("identityProvider") String identityProvider,
                                         @QueryParam("code") String authCode) throws ExternalIdpCallbackException {
        IdentityProvider idp = getIdentityProvider(identityProvider);
        final ValidationResult result = idp.handleCallback(authCode, getCallbackUri(identityProvider, "signin/callback"));
        try {
            final AuthTokenDTO tokenDTO = createTokenDTO(result.getCredentialsType(), result.getUserId());
            return createSignedInResponse(tokenDTO);
        } catch (ExternalIdpCallbackException e) {
            if (e.getCause() instanceof ObjectNotFoundException) {
                ObjectNotFoundException cause = (ObjectNotFoundException) e.getCause();
                final AuthTokenDTO tokenDTO = newSignup(result);
                return createSignedInResponse(tokenDTO);
            }
            throw e;
        } catch (Exception e) {
            throw new ExternalIdpCallbackException(e);
        }
    }

    private URI getCallbackUri(final String identityProvider, String path) {
        //TODO: Do not hard-code the hostname here:
        return URI.create("http://localhost:8080/api/openid/" + identityProvider + "/" + path);
    }

    @GET
    @Path("signup/callback")
    @UnitOfWork
    public Response handleSignUpCallback(@PathParam("identityProvider") String identityProvider,
                                         @QueryParam("code") String authCode,
                                         @QueryParam("state") String callbackState) throws ExternalIdpCallbackException {
        try {
            final JwtSignUpToken jwt = callbackStateTokenService.decode(callbackState);

            IdentityProvider idp = getIdentityProvider(identityProvider);
            final ValidationResult result = idp.handleCallback(authCode, getCallbackUri(identityProvider, "signup/callback"));
            if (jwt.getOrganizationId() != null) {
                final AuthTokenDTO tokenDTO = existingOrganizationSignup(jwt.getOrganizationId(), result);
                return createSignedInResponse(tokenDTO);
            } else if (jwt.getOrganizationName() != null) {
                final AuthTokenDTO tokenDTO = newOrganizationSignup(jwt.getOrganizationName(), result);
                return createSignedInResponse(tokenDTO);
            } else {
                final AuthTokenDTO tokenDTO = newSignup(result);
                return createSignedInResponse(tokenDTO);
            }
        } catch (ExternalIdpCallbackException e) {
            throw e;
        } catch (JwtTokenServiceException e) {
            throw new ExternalIdpCallbackException(new BadRequestException(e));
        } catch (Exception e) {
            throw new ExternalIdpCallbackException(e);
        }
    }

    private Response createSignedInResponse(AuthTokenDTO tokenDTO) {
        //TODO: Do not hard-code localhost in redirection URLs
        return Response.temporaryRedirect(URI.create("http://localhost:63344/#signin/" + tokenDTO.token)).build();
    }

    private IdentityProvider getIdentityProvider(String identityProvider) {
        if (identityProviders.containsKey(identityProvider)) {
            return identityProviders.get(identityProvider);
        } else {
            throw new NotFoundException();
        }
    }

    public AuthTokenDTO newOrganizationSignup(String newOrganizationName,
                                              ValidationResult validationResult) throws ExternalIdpCallbackException {

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

                final Credentials credentials = createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                return generateTokenResponse(credentials);
            } catch (DaoException e) {
                throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.SYSTEM_ERROR, "Could not create organization '" + newOrganizationName + "'.", e);
            }
        } else {
            throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.ORGANIZATION_EXISTS, "Organization " + newOrganizationName + " already exists.");
        }
    }

    private Credentials createCredentials(Person person, String userName, CredentialsType credentialsType, byte[] secret) throws DaoException {
        final CredentialsProperties credentialsProperties = new CredentialsProperties(StringUtils.defaultString(userName, person.getEmail()), credentialsType, secret);
        return credentialsDao.create(person, credentialsProperties);
    }

    public AuthTokenDTO existingOrganizationSignup(UuidString id,
                                                   ValidationResult validationResult) throws ExternalIdpCallbackException {
        try {
            if (Strings.isNullOrEmpty(validationResult.getUserEmail())) {
                throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.INVALID_INPUT, "E-mail cannot be empty");
            }
            try {
                final List<Person> people = peopleDao.getByEmail(validationResult.getUserEmail());
                if (people.size() == 1) {
                    final Person person = people.get(0);

                    Organization organization = organizationsDao.read(id.getUUID());
                    if (person.getOrganization().getId() == organization.getId()) {

                        final Credentials credentials = createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                        return generateTokenResponse(credentials);
                    } else {
                        throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.UNKNOWN_USER, validationResult.getUserEmail() + " is already registered with another organization.");
                    }
                } else if (people.isEmpty()) {
                    throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.INVALID_INPUT, "Cannot find " + validationResult.getUserEmail());
                } else {
                    throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.SYSTEM_ERROR, "E-mail address cannot be used to sign up as multiple people share the same e-mail address.");
                }
            } catch (DaoException e) {
                throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.SYSTEM_ERROR, "Could not configure user with credentials", e);
            }
        } catch (ObjectNotFoundException e) {
            throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.UNKNOWN_ORGANIZATION, "Could not find organization " + id, e);
        }
    }

    public AuthTokenDTO newSignup(ValidationResult validationResult) throws ExternalIdpCallbackException {
        if (Strings.isNullOrEmpty(validationResult.getUserEmail())) {
            throw new BadRequestException("Email cannot be empty");
        }
        try {
            final List<Person> people = peopleDao.getByEmail(validationResult.getUserEmail());
            if (people.size() == 1) {
                final Person person = people.get(0);

                final Credentials credentials = createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                return generateTokenResponse(credentials);
            } else if (people.isEmpty()) {
                throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.UNKNOWN_USER, "Cannot find " + validationResult.getUserEmail());
            } else {
                throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.SYSTEM_ERROR, "E-mail address cannot be used to sign up as multiple people share the same e-mail address.");
            }
        } catch (DaoException e) {
            throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.SYSTEM_ERROR, "Could not configure user with credentials", e);
        }
    }
}
