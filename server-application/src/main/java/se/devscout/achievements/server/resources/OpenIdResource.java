package se.devscout.achievements.server.resources;

import com.auth0.jwt.interfaces.DecodedJWT;
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

@Path("openid/{identityProvider}")
public class OpenIdResource extends AbstractResource {

    private final Map<String, IdentityProvider> MAPPING;
    private OpenIdResourceAuthUtil util;
    private JwtTokenService tokenService;

    public OpenIdResource(OpenIdResourceAuthUtil util,
                          JwtTokenService tokenService,
                          GoogleIdentityProvider googleIdentityProvider,
                          MicrosoftIdentityProvider microsoftIdentityProvider,
                          EmailIdentityProvider emailIdentityProvider) {
        MAPPING = ImmutableMap.<String, IdentityProvider>builder()
                .put(CredentialsType.GOOGLE.name().toLowerCase(), googleIdentityProvider)
                .put(CredentialsType.MICROSOFT.name().toLowerCase(), microsoftIdentityProvider)
                .put(CredentialsType.PASSWORD.name().toLowerCase(), emailIdentityProvider)
                .build();
        this.util = util;
        this.tokenService = tokenService;
    }

    @GET
    @Path("signin")
    @UnitOfWork
    public Response doSignInRequest(@PathParam("identityProvider") String identityProvider,
                                    @QueryParam("email") String email) {
        IdentityProvider idp = getIdentityProvider(identityProvider);
        final Map<String, String> claims = email != null ? ImmutableMap.of("email", email) : null;
        final String state = tokenService.encode(null, claims);
        return Response.temporaryRedirect(idp.getProviderAuthURL("signin/callback", state)).build();
    }

    @GET
    @Path("signup")
    @UnitOfWork
    public Response doSignUpRequest(@PathParam("identityProvider") String identityProvider,
                                    @QueryParam("organizationId") UuidString organizationId,
                                    @QueryParam("organizationName") String organizationName,
                                    @QueryParam("email") String email) {
        IdentityProvider idp = getIdentityProvider(identityProvider);
        final String state = tokenService.encode(null, ImmutableMap.of(
                "organizationId", organizationId != null ? organizationId.getValue() : null,
                "organizationName", organizationName,
                "email", email));
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
            final AuthTokenDTO tokenDTO = util.createToken(idp.getCredentialsType(), result.getUserId());
            return createSignedInResponse(tokenDTO);
        } catch (NotFoundException e) {
            final AuthTokenDTO tokenDTO = util.newSignup(result, idp.getCredentialsType(), idp.getCredentialsData());
            return createSignedInResponse(tokenDTO);
        }
    }

    @GET
    @Path("signup/callback")
    @UnitOfWork
    public Response handleSignUpCallback(@PathParam("identityProvider") String identityProvider,
                                         @QueryParam("code") String authCode,
                                         @QueryParam("state") String appState) {
        try {
            final DecodedJWT jwt = tokenService.decode(appState);

            IdentityProvider idp = getIdentityProvider(identityProvider);
            final ValidationResult result = idp.handleCallback(authCode, "signup/callback");
            if (!jwt.getClaim("organizationId").isNull()) {
                final AuthTokenDTO tokenDTO = util.existingOrganizationSignup(new UuidString(jwt.getClaim("organizationId").asString()), result, idp.getCredentialsType(), idp.getCredentialsData());
                return createSignedInResponse(tokenDTO);
            } else if (!jwt.getClaim("organizationName").isNull()) {
                final AuthTokenDTO tokenDTO = util.newOrganizationSignup(jwt.getClaim("organizationName").asString(), result, idp.getCredentialsType(), idp.getCredentialsData());
                return createSignedInResponse(tokenDTO);
            } else {
                final AuthTokenDTO tokenDTO = util.newSignup(result, idp.getCredentialsType(), idp.getCredentialsData());
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
