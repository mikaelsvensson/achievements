package se.devscout.achievements.server.auth.openid;

import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.model.CredentialsType;

import java.net.URI;

public interface IdentityProvider {
    URI getProviderAuthURL(String path, String appState);

    ValidationResult handleCallback(String authCode, String path);

    //TODO: Is IdentityProvider.getCredentialsType really necessary?
    CredentialsType getCredentialsType();

    //TODO: Is IdentityProvider.getCredentialsData really necessary?
    byte[] getCredentialsData();
}
