package se.devscout.achievements.server.auth.openid;

import org.apache.commons.lang3.RandomUtils;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.model.CredentialsType;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Base64;

abstract class AbstractIdentityProvider implements IdentityProvider {
    private String tokenEndpoint;
    private String authEndpoint;
    private String clientId;
    private String clientSecret;
    private Client client;
    private CredentialsValidator tokenValidator;

    public AbstractIdentityProvider(String authEndpoint, String clientId, String clientSecret, Client client, String tokenEndpoint, CredentialsValidator tokenValidator) {
        this.authEndpoint = authEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.client = client;
        this.tokenEndpoint = tokenEndpoint;
        this.tokenValidator = tokenValidator;
    }

    public URI getProviderAuthURL(String path, String appState) {
        return UriBuilder.fromUri(authEndpoint)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email")
                .queryParam("redirect_uri", getCallbackURL(path).toString())
                .queryParam("state", appState)
                //TODO: How and when is this nonce validated?
                .queryParam("nonce", Base64.getUrlEncoder().encodeToString(RandomUtils.nextBytes(30)))
                .build();
    }

    @Override
    public ValidationResult handleCallback(String authCode, String path) {
        final URI tokenUri = URI.create(tokenEndpoint);

        final MultivaluedHashMap<String, String> data = new MultivaluedHashMap<>();
        data.putSingle("client_id", clientId);
        data.putSingle("scope", "openid email");
        data.putSingle("grant_type", "authorization_code");
        data.putSingle("code", authCode);
        data.putSingle("redirect_uri", getCallbackURL(path).toString());
        data.putSingle("client_secret", clientSecret);

        final TokenResponse tokenResponse = client.
                target(tokenUri)
                .request()
                .post(Entity.form(data))
                .readEntity(TokenResponse.class);

        final String idToken = tokenResponse.id_token;

        return parseToken(idToken);
    }

    protected abstract URI getCallbackURL(String path);

    @Override
    public CredentialsType getCredentialsType() {
        return tokenValidator.getCredentialsType();
    }

    @Override
    public byte[] getCredentialsData() {
        return tokenValidator.getCredentialsData();
    }

    protected ValidationResult parseToken(String idToken) {
        return tokenValidator.validate(idToken.toCharArray());
    }
}
