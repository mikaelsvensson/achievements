package se.devscout.achievements.server.auth.email;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceException;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.resources.UuidString;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EmailIdentityProviderTest {

    private EmailIdentityProvider provider;

    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final HttpServletRequest req = mock(HttpServletRequest.class);
    private final HttpServletResponse resp = mock(HttpServletResponse.class);
    private CredentialsDao credentialsDao;

    @Before
    public void setUp() throws Exception {
        credentialsDao = mock(CredentialsDao.class);
        provider = new EmailIdentityProvider(jwtTokenService, emailSender, URI.create("http://gui"), credentialsDao);
    }

    @Test
    public void getRedirectUri_sendEmail_happyPath() throws IdentityProviderException, JwtTokenServiceException, EmailSenderException, ObjectNotFoundException {
        final var email = "alice@example.com";
        final var jwt = mockJwt();
        when(jwtTokenService.decode(eq("state"))).thenReturn(jwt);
        when(jwtTokenService.encode(eq(email), Matchers.isNull(Map.class), any(Duration.class))).thenReturn("emailToken");
        when(req.getParameter(eq("email"))).thenReturn(email);
        when(req.getParameter(eq("password"))).thenReturn(null);
        final var organization = MockUtil.mockOrganization("Acme");
        final var person = MockUtil.mockPerson(organization, "Alice", null, email, Roles.READER);
        final var credentials = MockUtil.mockCredentials(person, email);
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(email))).thenReturn(credentials);

        final var redirectUri = provider.getRedirectUri(req, resp, "state", URI.create("http://example.com/callback"));

        assertThat(redirectUri.getFragment()).isEqualTo("signin/check-mail-box");
        verify(jwtTokenService).encode(eq(email), Matchers.isNull(Map.class), any(Duration.class));
        verify(emailSender).send(anyString(), eq(email), anyString(), contains("?code=emailToken&state=state"));
    }

    @Test
    public void getRedirectUri_checkPassword_happyPath() throws IdentityProviderException, JwtTokenServiceException, EmailSenderException, ObjectNotFoundException {
        final var email = "alice@example.com";
        final var jwt = mockJwt();
        when(jwtTokenService.decode(eq("state"))).thenReturn(jwt);
        when(jwtTokenService.encode(eq(email), Matchers.isNull(Map.class), any(Duration.class))).thenReturn("emailToken");
        when(req.getParameter(eq("email"))).thenReturn(email);
        when(req.getParameter(eq("password"))).thenReturn("password");
        final var organization = MockUtil.mockOrganization("Acme");
        final var person = MockUtil.mockPerson(organization, "Alice", null, email, Roles.READER);
        final var credentials = MockUtil.mockCredentials(person, email);
        when(credentialsDao.get(eq(CredentialsType.PASSWORD), eq(email))).thenReturn(credentials);

        final var redirectUri = provider.getRedirectUri(req, resp, "state", URI.create("http://example.com/callback"));

        assertThat(redirectUri.toString()).isEqualTo("http://example.com/callback?code=emailToken&state=state");
        verify(jwtTokenService).encode(eq(email), Matchers.isNull(Map.class), any(Duration.class));
    }

    @Test(expected = IdentityProviderException.class)
    public void getRedirectUri_emailProblem() throws IdentityProviderException, JwtTokenServiceException, EmailSenderException {
        final var jwt = mockJwt();
        when(jwtTokenService.decode(eq("state"))).thenReturn(jwt);
        when(jwtTokenService.encode(eq("alice@example.com"), Matchers.isNull(Map.class), any(Duration.class))).thenReturn("emailToken");
        when(req.getParameter(eq("email"))).thenReturn("alice@example.com");
        doThrow(new EmailSenderException("error", new Exception())).when(emailSender).send(anyString(), anyString(), anyString(), anyString());

        try {
            provider.getRedirectUri(req, resp, "state", URI.create("http://example.com/callback"));
        } finally {
            verify(emailSender).send(anyString(), eq("alice@example.com"), anyString(), contains("?code=emailToken&state=state"));
        }
    }

    private DecodedJWT mockJwt() {
        final var jwt = mock(DecodedJWT.class);
        final var claim1 = mockClaim(new UuidString(UUID.randomUUID()).getValue());
        when(jwt.getClaim(eq(JwtSignInTokenService.ORGANIZATION_ID))).thenReturn(claim1);
        return jwt;
    }

    private Claim mockClaim(String reader) {
        final var mock = mock(Claim.class);
        when(mock.asString()).thenReturn(reader);
        return mock;
    }

    @Test
    public void handleCallback_happyPath() throws JwtTokenServiceException {
        final var jwt = mockJwt();
        when(jwtTokenService.decode(eq("authorization_code"))).thenReturn(jwt);
        when(req.getParameter(eq("code"))).thenReturn("authorization_code");
        try {
            provider.handleCallback(req, resp);
        } finally {
            verify(jwtTokenService).decode(eq("authorization_code"));
        }
    }

    @Test
    public void handleCallback_tokenProblem() throws JwtTokenServiceException {
        when(jwtTokenService.decode(eq("authorization_code"))).thenThrow(new JwtTokenServiceException(new Exception()));
        when(req.getParameter(eq("code"))).thenReturn("authorization_code");
        final var result = provider.handleCallback(req, resp);
        assertThat(result.isValid()).isFalse();
        verify(jwtTokenService).decode(eq("authorization_code"));
    }
}