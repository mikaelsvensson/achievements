package se.devscout.achievements.server.auth.openid;

import se.devscout.achievements.server.auth.microsoft.MicrosoftTokenValidator;

import javax.ws.rs.client.Client;
import java.net.URI;

public class MicrosoftIdentityProvider extends AbstractIdentityProvider {


    public MicrosoftIdentityProvider(String clientId, String clientSecret, Client client) {
        super(
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                clientId,
                clientSecret,
                client,
                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                new MicrosoftTokenValidator());
    }

    @Override
    public URI getCallbackURL(String path) {
        return URI.create("http://localhost:8080/api/openid/microsoft/" + path);
    }
}
