package se.devscout.achievements.server.auth.openid;

import javax.ws.rs.client.Client;
import java.net.URI;

public class GoogleIdentityProvider extends AbstractIdentityProvider {

    public GoogleIdentityProvider(String clientId, String clientSecret, Client client) {
        super(
                "https://accounts.google.com/o/oauth2/v2/auth",
                clientId,
                clientSecret,
                client,
                "https://www.googleapis.com/oauth2/v4/token",
                new GoogleTokenValidator(clientId));
    }

    @Override
    public URI getCallbackURL(String path) {
        return URI.create("http://localhost:8080/api/openid/google/" + path);
    }

}
