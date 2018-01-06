package se.devscout.achievements.server.auth;

import java.net.URI;

public interface IdentityProvider {
    URI getRedirectUri(String callbackState, URI callbackUri) throws IdentityProviderException;

    ValidationResult handleCallback(String authCode, URI callbackUri);
}
