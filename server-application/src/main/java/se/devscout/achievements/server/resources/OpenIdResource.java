package se.devscout.achievements.server.resources;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.TokenServiceException;
import se.devscout.achievements.server.auth.openid.EmailIdentityProvider;
import se.devscout.achievements.server.auth.openid.GoogleIdentityProvider;
import se.devscout.achievements.server.auth.openid.IdentityProvider;
import se.devscout.achievements.server.auth.openid.MicrosoftIdentityProvider;
import se.devscout.achievements.server.data.model.CredentialsType;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

//TODO: Test this resource!
//TODO: Rename resource since the "e-mail provider" is not an OpenID IdP.
@Path("openid/{identityProvider}")
public class OpenIdResource extends AbstractResource {

    private final Map<String, IdentityProvider> MAPPING;
    private OpenIdResourceAuthUtil util;
    private OpenIdCallbackStateTokenService tokenService;

    public OpenIdResource(OpenIdResourceAuthUtil util,
                          JwtTokenService tokenService,
                          GoogleIdentityProvider googleIdentityProvider,
                          MicrosoftIdentityProvider microsoftIdentityProvider,
                          EmailIdentityProvider emailIdentityProvider) {
        MAPPING = ImmutableMap.<String, IdentityProvider>builder()
                .put(CredentialsType.GOOGLE.name().toLowerCase(), googleIdentityProvider)
                .put(CredentialsType.MICROSOFT.name().toLowerCase(), microsoftIdentityProvider)
                //TODO: Rename to "email" instead of "password" since "password" is also the label we use in the database?
                .put(CredentialsType.PASSWORD.name().toLowerCase(), emailIdentityProvider)
                .build();
        this.util = util;
        this.tokenService = new OpenIdCallbackStateTokenService(tokenService);
    }

    @GET
    @Path("signin")
    @UnitOfWork
    public Response doSignInRequest(@PathParam("identityProvider") String identityProvider,
                                    @QueryParam("email") String email) {
        IdentityProvider idp = getIdentityProvider(identityProvider);
        final String state = tokenService.encode(new OpenIdCallbackStateToken(email));
        return Response.temporaryRedirect(idp.getProviderAuthURL("signin/callback", state)).build();
    }

    @GET
    @Path("signup")
    @UnitOfWork
    public Response doSignUpRequest(@PathParam("identityProvider") String identityProvider,
                                    @QueryParam("organization_id") UuidString organizationId,
                                    @QueryParam("new_organization_name") String organizationName,
                                    @QueryParam("email") String email) {
        IdentityProvider idp = getIdentityProvider(identityProvider);
        final String state = tokenService.encode(new OpenIdCallbackStateToken(email, organizationId, organizationName));
        return Response.temporaryRedirect(idp.getProviderAuthURL("signup/callback", state)).build();
    }


    @GET
    @Path("signin/callback")
    @UnitOfWork
    public Response handleSignInCallback(@PathParam("identityProvider") String identityProvider,
                                         @QueryParam("code") String authCode) {
        IdentityProvider idp = getIdentityProvider(identityProvider);
        final ValidationResult result = idp.handleCallback(authCode, "signin/callback");
        try {
            final AuthTokenDTO tokenDTO = util.createToken(result.getCredentialsType(), result.getUserId());
            return createSignedInResponse(tokenDTO);
        } catch (NotFoundException e) {
            final AuthTokenDTO tokenDTO = util.newSignup(result);
            return createSignedInResponse(tokenDTO);
        }
    }

    @GET
    @Path("signup/callback")
    @UnitOfWork
    public Response handleSignUpCallback(@PathParam("identityProvider") String identityProvider,
                                         @QueryParam("code") String authCode,
                                         @QueryParam("state") String callbackState) {
        try {
            final OpenIdCallbackStateToken jwt = tokenService.decode(callbackState);

            IdentityProvider idp = getIdentityProvider(identityProvider);
            final ValidationResult result = idp.handleCallback(authCode, "signup/callback");
            if (jwt.getOrganizationId() != null) {
                final AuthTokenDTO tokenDTO = util.existingOrganizationSignup(jwt.getOrganizationId(), result);
                return createSignedInResponse(tokenDTO);
            } else if (jwt.getOrganizationName() != null) {
                final AuthTokenDTO tokenDTO = util.newOrganizationSignup(jwt.getOrganizationName(), result);
                return createSignedInResponse(tokenDTO);
            } else {
                final AuthTokenDTO tokenDTO = util.newSignup(result);
                return createSignedInResponse(tokenDTO);
            }
        } catch (TokenServiceException e) {
            throw new BadRequestException();
        }
    }

    private Response createSignedInResponse(AuthTokenDTO tokenDTO) {
        //TODO: Do not hard-code localhost in redirection URLs
        return Response.temporaryRedirect(URI.create("http://localhost:63344/#signin/" + tokenDTO.token)).build();
    }

    private IdentityProvider getIdentityProvider(String identityProvider) {
        if (MAPPING.containsKey(identityProvider)) {
            return MAPPING.get(identityProvider);
        } else {
            throw new NotFoundException();
        }
    }
}
