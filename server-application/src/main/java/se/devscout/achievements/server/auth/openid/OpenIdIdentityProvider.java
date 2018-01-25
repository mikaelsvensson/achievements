package se.devscout.achievements.server.auth.openid;

import com.google.common.base.Strings;
import org.apache.commons.lang3.RandomUtils;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Base64;

public class OpenIdIdentityProvider implements IdentityProvider {
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

    public URI getRedirectUri(String callbackState, URI callbackUri) {
        return UriBuilder.fromUri(authEndpoint)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email")
                .queryParam("redirect_uri", callbackUri.toString())
                .queryParam("state", callbackState)
                //TODO: How and when is this nonce validated?
                .queryParam("nonce", Base64.getUrlEncoder().encodeToString(RandomUtils.nextBytes(30)))
                .build();
    }

    @Override
    public ValidationResult handleCallback(String authCode, URI callbackUri) throws IdentityProviderException {
        final URI tokenUri = URI.create(tokenEndpoint);

        final MultivaluedHashMap<String, String> data = new MultivaluedHashMap<>();
        data.putSingle("client_id", clientId);
        data.putSingle("scope", "openid email");
        data.putSingle("grant_type", "authorization_code");
        data.putSingle("code", authCode);
        data.putSingle("redirect_uri", callbackUri.toString());
        data.putSingle("client_secret", clientSecret);

        final OpenIdTokenResponse openIdTokenResponse = client
                .target(tokenUri)
//                .register(LOGGING_FEATURE)
                .request()
                .post(Entity.form(data))
                .readEntity(OpenIdTokenResponse.class);

        if (Strings.isNullOrEmpty(openIdTokenResponse.error)) {
            final String idToken = openIdTokenResponse.id_token;
            return parseToken(idToken);
        } else {
            throw new IdentityProviderException("Error when handling callback: " + openIdTokenResponse.error);
        }
    }

    protected ValidationResult parseToken(String idToken) {
        return tokenValidator.validate(idToken.toCharArray());
    }
}
