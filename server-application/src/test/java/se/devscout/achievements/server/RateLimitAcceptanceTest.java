package se.devscout.achievements.server;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimitAcceptanceTest {

    private static final int BURST_LENGTH = 5;
    private static final int MAX_REQUESTS_PER_SECOND = 4;
    private static final int REQUEST_DELAY = 1000 / MAX_REQUESTS_PER_SECOND;

    @Rule
    public final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"),
                    ConfigOverride.config("rateLimiting.requestsPerMinute", String.valueOf(MAX_REQUESTS_PER_SECOND * 60)),
                    ConfigOverride.config("rateLimiting.grace", String.valueOf(BURST_LENGTH)));

    @Test
    public void rateLimit_longBurst() {
        Client client = RULE.client();
        IntStream.rangeClosed(1, BURST_LENGTH + 5).forEach((i) -> {
            Response response1 = client
                    .target("http://localhost:9000/api/my/profile")
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .get();

            assertThat(response1.getStatus()).isEqualTo(i > BURST_LENGTH ? HttpStatus.TOO_MANY_REQUESTS_429 : HttpStatus.OK_200);
        });
    }

    @Test
    public void rateLimit_twoShortBurstsWithDelayInbetween() {
        Client client = RULE.client();
        IntStream.rangeClosed(1, BURST_LENGTH).forEach((i) -> {
            Response response1 = client
                    .target("http://localhost:9000/api/my/profile")
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .get();

            assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK_200);
        });
        try {
            System.err.println("A bit of sleep will do me good");
            Thread.sleep(REQUEST_DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        IntStream.rangeClosed(1, BURST_LENGTH).forEach((i) -> {
            Response response1 = client
                    .target("http://localhost:9000/api/my/profile")
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .get();

            assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK_200);
        });
    }

    @Test
    @Ignore("Exponential back-off not yet implemented")
    public void rateLimit_backOff() {
        Client client = RULE.client();
        IntStream.rangeClosed(1, BURST_LENGTH + 5).forEach((i) -> {
            try {
                final long wait = i > BURST_LENGTH ? (long) (Math.pow(1.5, i - BURST_LENGTH - 1) * REQUEST_DELAY) : 0;
                System.err.println("Waiting about " + wait + " ms.");
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Response response1 = client
                    .target("http://localhost:9000/api/my/profile")
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .get();

            assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK_200);
        });
    }
}
