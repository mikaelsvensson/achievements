package se.devscout.achievements.server.auth;

import java.net.URI;
import java.util.Map;

public interface IdentityProvider {
    URI getRedirectUri(String callbackState, URI callbackUri, Map<String, String> providerData) throws IdentityProviderException;

    ValidationResult handleCallback(String authCode, URI callbackUri) throws IdentityProviderException;
}
