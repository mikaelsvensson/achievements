package se.devscout.achievements.server.data.dao;

import io.dropwizard.auth.basic.BasicCredentials;
import org.junit.Before;
import org.junit.Test;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.IdentityProvider;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.authenticator.PasswordAuthenticator;
import se.devscout.achievements.server.resources.authenticator.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static se.devscout.achievements.server.MockUtil.mockOrganization;
import static se.devscout.achievements.server.MockUtil.mockPerson;

public class PasswordAuthenticatorTest {

    private CredentialsDao credentialsDao = mock(CredentialsDao.class);
    private PasswordAuthenticator authenticator = new PasswordAuthenticator(credentialsDao/*, new SecretValidatorFactory(null)*/);

    @Before
    public void setUp() throws Exception {
        final PasswordValidator passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray());
        final Organization organization = mockOrganization("Acme Inc.");
        final Person person = mockPerson(organization, "Alice");
        final Credentials credentials = new Credentials("username", passwordValidator.getIdentityProvider(), passwordValidator.getSecret(), person);
        when(credentialsDao.get(IdentityProvider.PASSWORD, "user")).thenReturn(credentials);
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