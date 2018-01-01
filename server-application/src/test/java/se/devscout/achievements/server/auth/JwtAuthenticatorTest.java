package se.devscout.achievements.server.auth;

import org.junit.Test;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceImpl;
import se.devscout.achievements.server.resources.authenticator.JwtAuthenticator;
import se.devscout.achievements.server.resources.authenticator.User;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtAuthenticatorTest {

    @Test
    public void generateTokenAndAuthenticate_happyPath() throws Exception {
        final JwtAuthenticator authenticator = new JwtAuthenticator(new JwtTokenServiceImpl("secret"));
        final String token = authenticator.generateToken("username", UUID.randomUUID(), 1337, Roles.EDITOR);
        final Optional<User> user = authenticator.authenticate(token);
        assertThat(user.get().getName()).isEqualTo("username");
    }

}