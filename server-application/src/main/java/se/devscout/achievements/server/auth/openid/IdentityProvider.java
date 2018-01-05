package se.devscout.achievements.server.auth.openid;

import se.devscout.achievements.server.auth.ValidationResult;

import java.net.URI;

public interface IdentityProvider {
    URI getProviderAuthURL(String callbackState, URI callbackUri) throws IdentityProviderException;

    ValidationResult handleCallback(String authCode, URI callbackUri);
}
