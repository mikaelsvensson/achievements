package se.devscout.achievements.server.resources;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimiterTest {

    private static final int BURST_LENGTH = 5;
    private static final int MAX_REQUESTS_PER_SECOND = 4;
    private static final int REQUEST_DELAY = 1000 / MAX_REQUESTS_PER_SECOND;

    private static final String ALICE = "Alice";

    private RateLimiter rateLimiter;

    @Before
    public void setUp() throws Exception {
        rateLimiter = new RateLimiter(
                MAX_REQUESTS_PER_SECOND * 60,
                BURST_LENGTH);
    }

    @Test
    public void rateLimit_longBurst() {
        IntStream.rangeClosed(1, BURST_LENGTH + 5).forEach((i) -> {
            final boolean isAccepted = rateLimiter.accept(ALICE);

            assertThat(isAccepted).isEqualTo(i <= BURST_LENGTH);
        });
    }

    @Test
    public void rateLimit_twoShortBurstsWithDelayInbetween() {
        IntStream.rangeClosed(1, BURST_LENGTH).forEach((i) -> {
            final boolean isAccepted = rateLimiter.accept(ALICE);

            assertThat(isAccepted).isTrue();
        });

        pause(REQUEST_DELAY);

        IntStream.rangeClosed(1, BURST_LENGTH).forEach((i) -> {
            final boolean isAccepted = rateLimiter.accept(ALICE);

            assertThat(isAccepted).isTrue();
        });
    }

    @Test
    @Ignore("Exponential back-off not yet implemented")
    public void rateLimit_backOff() {
        IntStream.rangeClosed(1, BURST_LENGTH + 5).forEach((i) -> {
            final long wait = i > BURST_LENGTH ? (long) (Math.pow(1.5, i - BURST_LENGTH - 1) * REQUEST_DELAY) : 0;

            pause(wait);

            final boolean isAccepted = rateLimiter.accept(ALICE);

            assertThat(isAccepted).isTrue();
        });
    }

    private void pause(long ms) {
        try {
            System.err.println("Waiting about " + ms + " ms.");
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}