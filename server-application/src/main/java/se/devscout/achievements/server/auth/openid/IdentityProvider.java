package se.devscout.achievements.server.auth.openid;

import se.devscout.achievements.server.auth.ValidationResult;

import java.net.URI;

public interface IdentityProvider {
    URI getProviderAuthURL(String path, String callbackState);

    ValidationResult handleCallback(String authCode, String path);
}
