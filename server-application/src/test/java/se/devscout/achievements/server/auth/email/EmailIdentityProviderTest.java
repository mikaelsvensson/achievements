package se.devscout.achievements.server.auth.email;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceException;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.resources.UuidString;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
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
        provider = new EmailIdentityProvider(jwtTokenService, emailSender, URI.create("http://gui"), mock(CredentialsDao.class));
    }

    @Test
    public void getRedirectUri_happyPath() throws IdentityProviderException, JwtTokenServiceException, EmailSenderException {
        final DecodedJWT jwt = mockJwt();
        when(jwtTokenService.decode(eq("state"))).thenReturn(jwt);
        when(jwtTokenService.encode(eq("alice@example.com"), Matchers.isNull(Map.class), any(Duration.class))).thenReturn("emailToken");

        final URI redirectUri = provider.getRedirectUri("state", URI.create("http://example.com/callback"), Collections.singletonMap("email", "alice@example.com"));

        assertThat(redirectUri.getFragment()).isEqualTo("signin/check-mail-box");
        verify(jwtTokenService).encode(eq("alice@example.com"), Matchers.isNull(Map.class), any(Duration.class));
        verify(emailSender).send(anyString(), eq("alice@example.com"), anyString(), contains("?code=emailToken&state=state"));
    }

    @Test(expected = IdentityProviderException.class)
    public void getRedirectUri_emailProblem() throws IdentityProviderException, JwtTokenServiceException, EmailSenderException {
        final DecodedJWT jwt = mockJwt();
        when(jwtTokenService.decode(eq("state"))).thenReturn(jwt);
        when(jwtTokenService.encode(eq("alice@example.com"), Matchers.isNull(Map.class), any(Duration.class))).thenReturn("emailToken");
        doThrow(new EmailSenderException("error", new Exception())).when(emailSender).send(anyString(), anyString(), anyString(), anyString());

        try {
            provider.getRedirectUri("state", URI.create("http://example.com/callback"), Collections.singletonMap("email", "alice@example.com"));
        } finally {
            verify(emailSender).send(anyString(), eq("alice@example.com"), anyString(), contains("?code=emailToken&state=state"));
        }
    }

    private DecodedJWT mockJwt() {
        final DecodedJWT jwt = mock(DecodedJWT.class);
        final Claim claim1 = mockClaim(new UuidString(UUID.randomUUID()).getValue());
        when(jwt.getClaim(eq(JwtSignInTokenService.ORGANIZATION_ID))).thenReturn(claim1);
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
            provider.handleCallback("authorization_code", URI.create("http://example.com/callback"));
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