package se.devscout.achievements.server.auth.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtTokenServiceImplTest {

    private JwtTokenServiceImpl tokenService;
    private JwtTokenServiceImpl badTokenService;

    @Before
    public void setUp() throws Exception {
        tokenService = new JwtTokenServiceImpl("my secret");
        badTokenService = new JwtTokenServiceImpl("another secret");
    }

    @Test
    public void encodeDecode_happyPath() throws JwtTokenServiceException {
        final String token = tokenService.encode("subject", Collections.singletonMap("key", "value"), Duration.ofHours(1));
        final DecodedJWT jwt = tokenService.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("subject");
        assertThat(jwt.getClaim("key").asString()).isEqualTo("value");
    }

    @Test(expected = JwtTokenServiceException.class)
    public void encodeDecode_wrongSecret() throws JwtTokenServiceException {
        final String token = badTokenService.encode("subject", Collections.singletonMap("key", "value"), Duration.ofHours(1));
        tokenService.decode(token);
    }

    @Test(expected = JwtTokenServiceException.class)
    public void encodeDecode_expiredToken() throws JwtTokenServiceException {
        final String token = tokenService.encode("subject", Collections.singletonMap("key", "value"), Duration.ofMinutes(-1));
        tokenService.decode(token);
    }
}