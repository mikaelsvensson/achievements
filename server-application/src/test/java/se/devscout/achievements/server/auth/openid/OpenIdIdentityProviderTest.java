package se.devscout.achievements.server.auth.openid;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.model.CredentialsType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class OpenIdIdentityProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    private HttpServletRequest req = mock(HttpServletRequest.class);
    private HttpServletResponse resp = mock(HttpServletResponse.class);
    private CredentialsValidator credentialsValidator = mock(CredentialsValidator.class);

    @Test
    public void handleCallback_expectedPropertiesOnly_happyPath() throws IdentityProviderException {
        String body = "{\"token_type\":\"Bearer\",\"scope\":\"openid email\",\"expires_in\":60,\"access_token\":\"accesstoken\",\"id_token\":\"idtoken\"}";

        handleCallback_happyPath(body);
    }

    @Test
    public void handleCallback_extraProperty_happyPath() throws IdentityProviderException {
        String body = "{\"custom_property\":\"value\",\"token_type\":\"Bearer\",\"scope\":\"openid email\",\"expires_in\":60,\"access_token\":\"accesstoken\",\"id_token\":\"idtoken\"}";

        handleCallback_happyPath(body);
    }

    private void handleCallback_happyPath(String tokenVerificationResponseBody) throws IdentityProviderException {
        stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(tokenVerificationResponseBody)));

        when(req.getParameter(eq("code"))).thenReturn("idp-token-code");
        when(req.getParameter(eq("state"))).thenReturn("callback-data");
        when(req.getRequestURI()).thenReturn("/auth/callback");
        when(credentialsValidator.validate(eq("idtoken".toCharArray()))).thenReturn(new ValidationResult("alice@example.com", "alice@example.com", true, CredentialsType.ONETIME_PASSWORD, null));

        final OpenIdIdentityProvider provider = new OpenIdIdentityProvider(
                UriBuilder.fromUri("http://localhost/auth").port(wireMockRule.port()).toString(),
                "client-id",
                "client-secret",
                UriBuilder.fromUri("http://localhost/token").port(wireMockRule.port()).toString(),
                credentialsValidator,
                URI.create("http://example.com")
        );

        final ValidationResult actual = provider.handleCallback(req, resp);

        assertThat(actual.getCallbackState()).isEqualTo("callback-data");
        assertThat(actual.getUserEmail()).isEqualTo("alice@example.com");

        verify(postRequestedFor(urlMatching("/token"))
                .withRequestBody(containing("code=idp-token-code")));
    }

    @Test
    public void handleCallback_errorCode_fail() throws IdentityProviderException {
        String body = "{\"token_type\":\"Bearer\",\"scope\":\"openid email\",\"expires_in\":60,\"error\":\"invalid_password\"}";

        handleCallback_fail(body);
    }

    @Test
    public void handleCallback_errorDescription_fail() throws IdentityProviderException {
        String body = "{\"token_type\":\"Bearer\",\"scope\":\"openid email\",\"expires_in\":60,\"error\":\"invalid_password\",\"error_description\":\"a long error message\"}";

        handleCallback_fail(body);
    }

    private void handleCallback_fail(String tokenVerificationResponseBody) throws IdentityProviderException {
        stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(tokenVerificationResponseBody)));

        when(req.getParameter(eq("code"))).thenReturn("idp-token-code");
        when(req.getParameter(eq("state"))).thenReturn("callback-data");
        when(req.getRequestURI()).thenReturn("/auth/callback");

        final OpenIdIdentityProvider provider = new OpenIdIdentityProvider(
                UriBuilder.fromUri("http://localhost/auth").port(wireMockRule.port()).toString(),
                "client-id",
                "client-secret",
                UriBuilder.fromUri("http://localhost/token").port(wireMockRule.port()).toString(),
                credentialsValidator,
                URI.create("http://example.com")
        );

        try {
            provider.handleCallback(req, resp);
            fail("handleCallback should have thrown exception");
        } catch (IdentityProviderException e) {
            // Expected
        }

        Mockito.verify(credentialsValidator, never()).validate(Matchers.any(char[].class));

        verify(postRequestedFor(urlMatching("/token"))
                .withRequestBody(containing("code=idp-token-code")));
    }
}