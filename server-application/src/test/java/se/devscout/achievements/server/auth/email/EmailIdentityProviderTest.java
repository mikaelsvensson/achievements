package se.devscout.achievements.server.auth.email;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceException;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.resources.UuidString;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class EmailIdentityProviderTest {

    private EmailIdentityProvider provider;

    private JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private EmailSender emailSender = mock(EmailSender.class);

    @Before
    public void setUp() throws Exception {
        provider = new EmailIdentityProvider(jwtTokenService, emailSender, URI.create("http://gui"));
    }

    @Test
    public void getRedirectUri_happyPath() throws IdentityProviderException, JwtTokenServiceException, EmailSenderException {
        final DecodedJWT jwt = mockJwt();
        when(jwtTokenService.decode(eq("state"))).thenReturn(jwt);

        final URI redirectUri = provider.getRedirectUri("state", URI.create("http://example.com/callback"));

        assertThat(redirectUri.getFragment()).isEqualTo("signin/check-mail-box");
        verify(jwtTokenService).decode(eq("state"));
        verify(emailSender).send(eq("alice@example.com"), anyString(), contains("?code=state"));
    }

    @Test(expected = IdentityProviderException.class)
    public void getRedirectUri_tokenProblem() throws IdentityProviderException, JwtTokenServiceException, EmailSenderException {
        when(jwtTokenService.decode(eq("state"))).thenThrow(new JwtTokenServiceException(new Exception()));

        try {
            provider.getRedirectUri("state", URI.create("http://example.com/callback"));
        } finally {
            verify(jwtTokenService).decode(eq("state"));
            verify(emailSender, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Test(expected = IdentityProviderException.class)
    public void getRedirectUri_emailProblem() throws IdentityProviderException, JwtTokenServiceException, EmailSenderException {
        final DecodedJWT jwt = mockJwt();
        when(jwtTokenService.decode(eq("state"))).thenReturn(jwt);
        doThrow(new EmailSenderException("error", new Exception())).when(emailSender).send(anyString(), anyString(), anyString());

        try {
            provider.getRedirectUri("state", URI.create("http://example.com/callback"));
        } finally {
            verify(jwtTokenService).decode(eq("state"));
            verify(emailSender).send(eq("alice@example.com"), anyString(), contains("?code=state"));
        }
    }

    private DecodedJWT mockJwt() {
        final DecodedJWT jwt = mock(DecodedJWT.class);
        final Claim claim1 = mockClaim(new UuidString(UUID.randomUUID()).getValue());
        final Claim claim2 = mockClaim("alice@example.com");
        when(jwt.getClaims()).thenReturn(ImmutableMap.of(JwtSignInTokenService.ORGANIZATION_ID, claim1, JwtSignInTokenService.EMAIL, claim2));
        when(jwt.getClaim(eq(JwtSignInTokenService.ORGANIZATION_ID))).thenReturn(claim1);
        when(jwt.getClaim(eq(JwtSignInTokenService.EMAIL))).thenReturn(claim2);
        return jwt;
    }

    private Claim mockClaim(String reader) {
        final Claim mock = mock(Claim.class);
        when(mock.asString()).thenReturn(reader);
        return mock;
    }

    @Test
    public void handleCallback_happyPath() throws JwtTokenServiceException {
        final DecodedJWT jwt = mockJwt();
        when(jwtTokenService.decode(eq("authorization_code"))).thenReturn(jwt);
        try {
            final ValidationResult result = provider.handleCallback("authorization_code", URI.create("http://example.com/callback"));
            assertThat(result.getUserEmail()).isEqualTo(jwt.getClaim(JwtSignInTokenService.EMAIL).asString());
        } finally {
            verify(jwtTokenService).decode(eq("authorization_code"));
        }
    }

    @Test
    public void handleCallback_tokenProblem() throws JwtTokenServiceException {
        when(jwtTokenService.decode(eq("authorization_code"))).thenThrow(new JwtTokenServiceException(new Exception()));
        final ValidationResult result = provider.handleCallback("authorization_code", URI.create("http://example.com/callback"));
        assertThat(result.isValid()).isFalse();
        verify(jwtTokenService).decode(eq("authorization_code"));
    }
}