package se.devscout.achievements.server.auth.jwt;

import org.junit.Test;
import se.devscout.achievements.server.auth.Roles;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtSignInTokenServiceTest {

    @Test
    public void generateTokenAndAuthenticate_happyPath() throws Exception {
        final JwtSignInTokenService signInTokenService = new JwtSignInTokenService(new JwtTokenServiceImpl("secret"));
        final UUID credentialsId = UUID.randomUUID();

        final JwtSignInToken signInToken = new JwtSignInToken(
                "username",
                1337,
                credentialsId,
                Collections.singleton(Roles.EDITOR));

        final String token = signInTokenService.encode(signInToken);
        final JwtSignInToken user = signInTokenService.decode(token);

        assertThat(user.getPersonName()).isEqualTo("username");
        assertThat(user.getPersonId()).isEqualTo(1337);
        assertThat(user.getCredentialsId()).isEqualTo(credentialsId);
        assertThat(user.getRoles()).containsOnly(Roles.EDITOR);
    }

}