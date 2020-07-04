package se.devscout.achievements.server.auth.jwt;

import org.junit.Test;
import se.devscout.achievements.server.auth.Roles;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtSignInTokenServiceTest {

    @Test
    public void generateTokenAndAuthenticate_happyPath() throws Exception {
        final var signInTokenService = new JwtSignInTokenService(new JwtTokenServiceImpl("secret"));
        final var credentialsId = UUID.randomUUID();
        final var organizationId = UUID.randomUUID();

        final var signInToken = new JwtSignInToken(
                "username",
                1337,
                credentialsId,
                Collections.singleton(Roles.EDITOR), organizationId);

        final var token = signInTokenService.encode(signInToken);
        final var user = signInTokenService.decode(token);

        assertThat(user.getPersonName()).isEqualTo("username");
        assertThat(user.getPersonId()).isEqualTo(1337);
        assertThat(user.getCredentialsId()).isEqualTo(credentialsId);
        assertThat(user.getRoles()).containsOnly(Roles.EDITOR);
        assertThat(user.getOrganizationId()).isEqualTo(organizationId);
    }

}