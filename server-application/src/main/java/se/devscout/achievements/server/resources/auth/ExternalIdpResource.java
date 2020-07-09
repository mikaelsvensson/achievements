package se.devscout.achievements.server.resources.auth;

import com.google.common.base.Strings;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.I18n;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtSignUpToken;
import se.devscout.achievements.server.auth.jwt.JwtSignUpTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceException;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.mail.template.WelcomeOrganizationTemplate;
import se.devscout.achievements.server.resources.UuidString;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static se.devscout.achievements.server.resources.auth.ExternalIdpCallbackExceptionType.*;

//TODO: Test this resource!
@Path("auth/{identityProvider}")
public class ExternalIdpResource extends AbstractAuthResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalIdpResource.class);
    private final Map<String, IdentityProvider> identityProviders;
    //TODO: Two utility classes for dealing with tokens seems a bit much, doesn't it?
    private final JwtSignUpTokenService callbackStateTokenService;
    private final CredentialsDao credentialsDao;
    private final PeopleDao peopleDao;
    private final OrganizationsDao organizationsDao;
    private final URI guiApplicationHost;
    private final URI serverApplicationHost;
    private final EmailSender emailSender;
    private final I18n i18n;

    public ExternalIdpResource(Map<String, IdentityProvider> identityProviders,
                               CredentialsDao credentialsDao,
                               PeopleDao peopleDao,
                               OrganizationsDao organizationsDao,
                               URI guiApplicationHost,
                               URI serverApplicationHost,
                               JwtSignInTokenService signInTokenService,
                               JwtSignUpTokenService signUpTokenService,
                               EmailSender emailSender,
                               I18n i18n) {
        super(signInTokenService, credentialsDao);
        this.identityProviders = identityProviders;
        this.callbackStateTokenService = signUpTokenService;
        this.credentialsDao = credentialsDao;
        this.peopleDao = peopleDao;
        this.organizationsDao = organizationsDao;
        this.guiApplicationHost = guiApplicationHost;
        this.serverApplicationHost = serverApplicationHost;
        this.emailSender = emailSender;
        this.i18n = i18n;
    }

    @GET
    @Path("metadata")
    @UnitOfWork
    public Response doMetadataRequest(@PathParam("identityProvider") String identityProvider,
                                      @Context HttpServletRequest req,
                                      @Context HttpServletResponse res) throws ExternalIdpCallbackException {
        try {
            var idp = getIdentityProvider(identityProvider);
            final var response = idp.getMetadataResponse();
            return response != null ? response : Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            throw new ExternalIdpCallbackException(e);
        }
    }

    @POST
    @Path("signin")
    @UnitOfWork
    public Response doSignInRequest(@PathParam("identityProvider") String identityProvider,
                                    @Context HttpServletRequest req,
                                    @Context HttpServletResponse res) throws ExternalIdpCallbackException {
        try {
            var idp = getIdentityProvider(identityProvider);
            final var state = callbackStateTokenService.encode(new JwtSignUpToken(null, null));
            return Response.seeOther(idp.getRedirectUri(req, res, state, getCallbackUri(identityProvider, "signin/callback"))).build();
        } catch (Exception e) {
            throw new ExternalIdpCallbackException(e);
        }
    }

    @POST
    @Path("signup")
    @UnitOfWork
    public Response doSignUpRequest(@PathParam("identityProvider") String identityProvider,
                                    @FormParam("organization_id") UuidString organizationId,
                                    @FormParam("new_organization_name") String organizationName,
                                    @Context HttpServletRequest req,
                                    @Context HttpServletResponse res) throws ExternalIdpCallbackException {
        try {
            var idp = getIdentityProvider(identityProvider);
            final var state = callbackStateTokenService.encode(new JwtSignUpToken(organizationId, organizationName));
            return Response.seeOther(idp.getRedirectUri(req, res, state, getCallbackUri(identityProvider, "signup/callback"))).build();
        } catch (Exception e) {
            throw new ExternalIdpCallbackException(e);
        }
    }


    @GET
    @Path("signin/callback")
    @UnitOfWork
    public Response handleSignInCallback(@PathParam("identityProvider") String identityProvider,
                                         @Context HttpServletRequest req,
                                         @Context HttpServletResponse res) throws ExternalIdpCallbackException {
        var idp = getIdentityProvider(identityProvider);
        try {
            final var result = idp.handleCallback(req, res);
            try {
                final var tokenDTO = createTokenDTO(result.getCredentialsType(), result.getUserId());
                return createSignedInResponse(tokenDTO);
            } catch (ExternalIdpCallbackException e) {
                if (e.getType() == UNKNOWN_USER) {
                    final var tokenDTO = newSignup(result);
                    return createSignedInResponse(tokenDTO);
                }
                throw e;
            }
        } catch (ExternalIdpCallbackException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalIdpCallbackException(e);
        }
    }

    @POST
    @Path("signin/callback")
    @UnitOfWork
    public Response handlePostedSignInCallback(@PathParam("identityProvider") String identityProvider,
                                               @Context HttpServletRequest req,
                                               @Context HttpServletResponse res) throws ExternalIdpCallbackException {
        return handleSignInCallback(identityProvider, req, res);
    }

    private URI getCallbackUri(final String identityProvider, String path) {
        return URI.create(StringUtils.appendIfMissing(serverApplicationHost.toString(), "/") + "api/auth/" + identityProvider + "/" + path);
    }

    @GET
    @Path("signup/callback")
    @UnitOfWork
    public Response handleSignUpCallback(@PathParam("identityProvider") String identityProvider,
                                         @Context HttpServletRequest req,
                                         @Context HttpServletResponse res) throws ExternalIdpCallbackException {
        try {

            var idp = getIdentityProvider(identityProvider);
            final var result = idp.handleCallback(req, res);
            final var jwt = callbackStateTokenService.decode(result.getCallbackState());
            if (jwt.getOrganizationId() != null) {
                final var tokenDTO = existingOrganizationSignup(jwt.getOrganizationId(), result);
                return createSignedInResponse(tokenDTO);
            } else if (jwt.getOrganizationName() != null) {
                final var tokenDTO = newOrganizationSignup(jwt.getOrganizationName(), result, req);
                return createSignedInResponse(tokenDTO);
            } else {
                // When does this happen? Signing up for the site without an organization? Visiting the home page and logging in using an email already in the system?
                final var tokenDTO = newSignup(result);
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
        return Response.seeOther(guiUri("signin/" + tokenDTO.token)).build();
    }

    private IdentityProvider getIdentityProvider(String identityProvider) {
        if (identityProviders.containsKey(identityProvider)) {
            return identityProviders.get(identityProvider);
        } else {
            throw new NotFoundException();
        }
    }

    private AuthTokenDTO newOrganizationSignup(String newOrganizationName,
                                               ValidationResult validationResult,
                                               HttpServletRequest request) throws ExternalIdpCallbackException {

        if (Strings.isNullOrEmpty(newOrganizationName)) {
            throw new BadRequestException("Name of new organization was not specified.");
        }
        final var organizations = organizationsDao.find(newOrganizationName);
        if (organizations.isEmpty()) {
            try {
                // Create organization
                final var organization = organizationsDao.create(new OrganizationProperties(newOrganizationName));

                // Create person
                final var email = validationResult.getUserEmail();
                final var name = StringUtils.substringBefore(email, "@");
                final var person = peopleDao.create(organization, new PersonProperties(name, email, Collections.emptySet(), null, Roles.EDITOR));

                sendOrganizationWelcomeMail(email, request, organization.getId());

                final var credentials = createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                return generateTokenResponse(credentials);
            } catch (DaoException e) {
                throw new ExternalIdpCallbackException(SYSTEM_ERROR, "Could not create organization '" + newOrganizationName + "'.", e);
            }
        } else {
            throw new ExternalIdpCallbackException(ORGANIZATION_EXISTS, "Organization " + newOrganizationName + " already exists.");
        }
    }

    private void sendOrganizationWelcomeMail(String to, HttpServletRequest request, UUID orgId) {
        try {
            final var body = new WelcomeOrganizationTemplate().render(
                    guiUri("marken"),
                    guiUri("karer/" + UuidString.toString(orgId)),
                    guiUri("karer/" + UuidString.toString(orgId) + "/importera"),
                    guiUri("karer/" + UuidString.toString(orgId)),
                    guiUri("om"));

            emailSender.send(
                    request != null ? request.getRemoteAddr() : "ANONYMOUS",
                    to,
                    i18n.get("sendOrganizationWelcomeMail.subject"),
                    body);
            // TODO: Save e-mail in database
        } catch (EmailSenderException e) {
            LOGGER.warn("Could not send welcome mail to " + to, e);
        }
    }

    private URI guiUri(String fragment) {
        return URI.create(StringUtils.appendIfMissing(guiApplicationHost.toString(), "/") + "#" + fragment);
    }

    private Credentials createCredentials(Person person, String userName, CredentialsType credentialsType, byte[] secret) throws DaoException {
        final var credentialsProperties = new CredentialsProperties(StringUtils.defaultString(userName, person.getEmail()), credentialsType, secret);
        return credentialsDao.create(person, credentialsProperties);
    }

    // TODO: existingOrganizationSignup and newSignup are almost identical in functionality (only difference is organization identifier check? and what good does that check actually do?)
    private AuthTokenDTO existingOrganizationSignup(UuidString id,
                                                    ValidationResult validationResult) throws ExternalIdpCallbackException {
        try {
            if (Strings.isNullOrEmpty(validationResult.getUserEmail())) {
                throw new ExternalIdpCallbackException(INVALID_INPUT, "E-mail cannot be empty");
            }
            try {
                final var people = peopleDao.getByEmail(validationResult.getUserEmail());
                if (people.size() <= 1) {
                    var organization = organizationsDao.read(id.getUUID());

                    final Person person;
                    if (people.size() == 0) {
                        // (1) Create new person and...
                        final var email = validationResult.getUserEmail();
                        final var name = StringUtils.substringBefore(email, "@");
                        person = peopleDao.create(organization, new PersonProperties(name, email, Collections.emptySet(), null, Roles.READER));
                    } else {
                        // (1) Use existing person and...
                        person = people.get(0);
                        if (person.getOrganization().getId() != organization.getId()) {
                            throw new ExternalIdpCallbackException(UNKNOWN_USER, validationResult.getUserEmail() + " is already registered with another organization.");
                        }
                    }

                    // (2) ...associate credentials with this person.
                    final var credentials = createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                    return generateTokenResponse(credentials);
                } else {
                    throw new ExternalIdpCallbackException(SYSTEM_ERROR, "E-mail address cannot be used to sign up as multiple people share the same e-mail address.");
                }
            } catch (DaoException e) {
                throw new ExternalIdpCallbackException(SYSTEM_ERROR, "Could not configure user with credentials", e);
            }
        } catch (ObjectNotFoundException e) {
            throw new ExternalIdpCallbackException(UNKNOWN_ORGANIZATION, "Could not find organization " + id, e);
        }
    }

    private AuthTokenDTO newSignup(ValidationResult validationResult) throws ExternalIdpCallbackException {
        if (Strings.isNullOrEmpty(validationResult.getUserEmail())) {
            throw new ExternalIdpCallbackException(INVALID_INPUT, "E-mail cannot be empty");
        }
        try {
            final var people = peopleDao.getByEmail(validationResult.getUserEmail());
            if (people.size() == 1) {
                final var person = people.get(0);

                final var credentials = createCredentials(person, validationResult.getUserId(), validationResult.getCredentialsType(), validationResult.getCredentialsData());

                return generateTokenResponse(credentials);
            } else if (people.isEmpty()) {
                throw new ExternalIdpCallbackException(UNKNOWN_USER, "Cannot find " + validationResult.getUserEmail());
            } else {
                throw new ExternalIdpCallbackException(SYSTEM_ERROR, "E-mail address cannot be used to sign up as multiple people share the same e-mail address.");
            }
        } catch (DaoException e) {
            throw new ExternalIdpCallbackException(SYSTEM_ERROR, "Could not configure user with credentials", e);
        }
    }
}
