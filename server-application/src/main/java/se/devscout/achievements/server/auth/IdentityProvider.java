package se.devscout.achievements.server.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

public interface IdentityProvider {
    URI getRedirectUri(HttpServletRequest req, HttpServletResponse resp, String callbackState, URI callbackUri) throws IdentityProviderException;

    ValidationResult handleCallback(HttpServletRequest req, HttpServletResponse resp) throws IdentityProviderException;

    Response getMetadataResponse() throws IdentityProviderException;
}
