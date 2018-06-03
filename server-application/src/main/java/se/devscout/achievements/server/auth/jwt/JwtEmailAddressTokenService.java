package se.devscout.achievements.server.auth.jwt;

import java.time.Duration;

public class JwtEmailAddressTokenService {

    private static final Duration DURATION_15_MINS = Duration.ofMinutes(15);

    private final JwtTokenService jwtTokenService;

    public JwtEmailAddressTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public String encode(JwtEmailAddressToken state) {
        return jwtTokenService.encode(state.getEmail(), null, DURATION_15_MINS);
    }

    public JwtEmailAddressToken decode(String token) throws JwtTokenServiceException {
        return new JwtEmailAddressToken(jwtTokenService.decode(token).getSubject());
    }
}
