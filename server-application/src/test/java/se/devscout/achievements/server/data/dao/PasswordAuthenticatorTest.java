package se.devscout.achievements.server.data.dao;

import io.dropwizard.auth.basic.BasicCredentials;
import org.junit.Before;
import org.junit.Test;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.resources.authenticator.PasswordAuthenticator;
import se.devscout.achievements.server.resources.authenticator.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PasswordAuthenticatorTest {

    private CredentialsDao credentialsDao = mock(CredentialsDao.class);
    private PasswordAuthenticator authenticator = new PasswordAuthenticator(credentialsDao/*, new SecretValidatorFactory(null)*/);

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
        when(credentialsDao.get(CredentialsType.PASSWORD, "missing")).thenThrow(new ObjectNotFoundException());
    }

    @Test
    public void authenticate_correctUsernameAndPassword_happyPath() throws Exception {
        final Optional<User> actual = authenticator.authenticate(new BasicCredentials(MockUtil.USERNAME_READER, "password"));

        assertThat(actual.isPresent()).isTrue();
    }

    @Test
    public void authenticate_unknownUser_happyPath() throws Exception {
        final Optional<User> actual = authenticator.authenticate(new BasicCredentials("missing", "password"));

        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    public void authenticate_wrongPassword_happyPath() throws Exception {
        final Optional<User> actual = authenticator.authenticate(new BasicCredentials(MockUtil.USERNAME_READER, "wrong password"));

        assertThat(actual.isPresent()).isFalse();
    }
}