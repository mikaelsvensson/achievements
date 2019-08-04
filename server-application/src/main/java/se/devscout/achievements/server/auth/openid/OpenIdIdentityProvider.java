package se.devscout.achievements.server.auth.openid;

import com.google.common.base.Strings;
import org.apache.commons.lang3.RandomUtils;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Base64;

public class OpenIdIdentityProvider implements IdentityProvider {
    private static final String OPENID_APP_STATE_PARAM = "state";
    private static final String OPENID_IDP_STATE_PARAM = "code";
    //    public static final LoggingFeature LOGGING_FEATURE = new LoggingFeature(
//            Logger.getLogger(OpenIdIdentityProvider.class.getName()),
//            Level.INFO,
//            LoggingFeature.Verbosity.PAYLOAD_TEXT,
//            10_000);
    private String tokenEndpoint;
    private String authEndpoint;
    private String clientId;
    private String clientSecret;
    private Client client;
    private CredentialsValidator tokenValidator;

    public OpenIdIdentityProvider(String authEndpoint, String clientId, String clientSecret, Client client, String tokenEndpoint, CredentialsValidator tokenValidator) {
        this.authEndpoint = authEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.client = client;
        this.tokenEndpoint = tokenEndpoint;
        this.tokenValidator = tokenValidator;
    }

    @Override
    public URI getRedirectUri(HttpServletRequest req, HttpServletResponse resp, String callbackState, URI callbackUri) {
        return UriBuilder.fromUri(authEndpoint)
                .queryParam("client_id", clientId)
                .queryParam("response_type", OPENID_IDP_STATE_PARAM)
                .queryParam("scope", "openid email")
                .queryParam("redirect_uri", callbackUri.toString())
                .queryParam(OPENID_APP_STATE_PARAM, callbackState)
                //TODO: How and when is this nonce validated?
                .queryParam("nonce", Base64.getUrlEncoder().encodeToString(RandomUtils.nextBytes(30)))
                .build();
    }

    @Override
    public ValidationResult handleCallback(HttpServletRequest req, HttpServletResponse resp) throws IdentityProviderException {
        final URI tokenUri = URI.create(tokenEndpoint);

        final MultivaluedHashMap<String, String> data = new MultivaluedHashMap<>();
        data.putSingle("client_id", clientId);
        data.putSingle("scope", "openid email");
        data.putSingle("grant_type", "authorization_code");
        data.putSingle(OPENID_IDP_STATE_PARAM, req.getParameter(OPENID_IDP_STATE_PARAM));
        data.putSingle("redirect_uri", req.getRequestURI());
        data.putSingle("client_secret", clientSecret);

        final OpenIdTokenResponse openIdTokenResponse = client
                .target(tokenUri)
//                .register(LOGGING_FEATURE)
                .request()
                .post(Entity.form(data))
                .readEntity(OpenIdTokenResponse.class);

        if (Strings.isNullOrEmpty(openIdTokenResponse.error)) {
            final String idToken = openIdTokenResponse.id_token;
            return parseToken(idToken).withCallbackState(req.getParameter(OPENID_APP_STATE_PARAM));
        } else {
            throw new IdentityProviderException("Error when handling callback: " + openIdTokenResponse.error);
        }
    }

    @Override
    public Response getMetadataResponse() {
        return null;
    }

    protected ValidationResult parseToken(String idToken) {
        return tokenValidator.validate(idToken.toCharArray());
    }
}
