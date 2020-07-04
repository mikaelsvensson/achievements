package se.devscout.achievements.server.auth.openid;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.base.Strings;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Base64;

//import org.glassfish.jersey.logging.LoggingFeature;
//import java.util.logging.Level;
//import java.util.logging.Logger;

public class OpenIdIdentityProvider implements IdentityProvider {
    private static final String OPENID_APP_STATE_PARAM = "state";
    private static final String OPENID_IDP_STATE_PARAM = "code";
    //    public static final LoggingFeature LOGGING_FEATURE = new LoggingFeature(
//            Logger.getLogger(OpenIdIdentityProvider.class.getName()),
//            Level.INFO,
//            LoggingFeature.Verbosity.PAYLOAD_TEXT,
//            10_000);
    private final String tokenEndpoint;
    private final String authEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final Client httpClient;
    private final CredentialsValidator tokenValidator;
    private final URI serverApplicationHost;

    public OpenIdIdentityProvider(String authEndpoint, String clientId, String clientSecret, String tokenEndpoint, CredentialsValidator tokenValidator, URI serverApplicationHost) {
        this.authEndpoint = authEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = createHttpClient();
        this.tokenEndpoint = tokenEndpoint;
        this.tokenValidator = tokenValidator;
        this.serverApplicationHost = serverApplicationHost;
    }

    private Client createHttpClient() {
        final var jacksonJsonProvider = new JacksonJaxbJsonProvider()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return ClientBuilder.newClient(new ClientConfig(jacksonJsonProvider));
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
        // invokedCallbackUri needs to match what we sent in getRedirectUri(...)
        final var invokedCallbackUri = UriBuilder
                .fromUri(this.serverApplicationHost)
                .path(req.getRequestURI())
                .build()
                .toString();

        final var data = new MultivaluedHashMap<String, String>();
        data.putSingle("client_id", clientId);
        data.putSingle("scope", "openid email");
        data.putSingle("grant_type", "authorization_code");
        data.putSingle(OPENID_IDP_STATE_PARAM, req.getParameter(OPENID_IDP_STATE_PARAM));
        data.putSingle("redirect_uri", invokedCallbackUri);
        data.putSingle("client_secret", clientSecret);

        final var tokenUri = URI.create(tokenEndpoint);
        final var openIdTokenResponse = httpClient
                .target(tokenUri)
//                .register(LOGGING_FEATURE)
                .request()
                .post(Entity.form(data))
                .readEntity(OpenIdTokenResponse.class);

        final var error = StringUtils.defaultString(openIdTokenResponse.error_description, openIdTokenResponse.error);
        if (Strings.isNullOrEmpty(error)) {
            final var idToken = openIdTokenResponse.id_token;
            return parseToken(idToken).withCallbackState(req.getParameter(OPENID_APP_STATE_PARAM));
        } else {
            throw new IdentityProviderException("Error when handling callback: " + error);
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
