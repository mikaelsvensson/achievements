package se.devscout.achievements.server.auth;

import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtAuthenticatorTest {

    @Test
    public void generateTokenAndAuthenticate_happyPath() throws Exception {
        final JwtAuthenticator authenticator = new JwtAuthenticator("secret");
        final String token = authenticator.generateToken("username", UUID.randomUUID(), 1337);
        final Optional<User> user = authenticator.authenticate(token);
        assertThat(user.get().getName()).isEqualTo("username");
    }

}