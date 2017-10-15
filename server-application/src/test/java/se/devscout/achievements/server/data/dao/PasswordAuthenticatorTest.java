package se.devscout.achievements.server.data.dao;

import io.dropwizard.auth.basic.BasicCredentials;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import se.devscout.achievements.server.auth.PasswordAuthenticator;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;
import se.devscout.achievements.server.auth.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PasswordAuthenticatorTest {

    private CredentialsDao credentialsDao = mock(CredentialsDao.class);
    private PasswordAuthenticator authenticator = new PasswordAuthenticator(mock(SessionFactory.class), credentialsDao);

    @Before
    public void setUp() throws Exception {
        when(credentialsDao.get(IdentityProvider.PASSWORD, "user")).thenReturn(new Credentials("username", new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray())));
        when(credentialsDao.get(IdentityProvider.PASSWORD, "missing")).thenThrow(new ObjectNotFoundException());
    }

    @Test
    public void authenticate_correctUsernameAndPassword_happyPath() throws Exception {
        final Optional<User> actual = authenticator.authenticate(new BasicCredentials("user", "password"));

        assertThat(actual.isPresent()).isTrue();
    }

    @Test
    public void authenticate_unknownUser_happyPath() throws Exception {
        final Optional<User> actual = authenticator.authenticate(new BasicCredentials("missing", "password"));

        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    public void authenticate_wrongPassword_happyPath() throws Exception {
        final Optional<User> actual = authenticator.authenticate(new BasicCredentials("user", "wrong password"));

        assertThat(actual.isPresent()).isFalse();
    }
}