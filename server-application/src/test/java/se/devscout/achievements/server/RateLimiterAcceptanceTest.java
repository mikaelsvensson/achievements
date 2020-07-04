package se.devscout.achievements.server;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.resources.ForgotPasswordDTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimiterAcceptanceTest {

    private static final int BURST_LENGTH = 5;
    private static final int MAX_REQUESTS_PER_SECOND = 4;
    private static final int REQUEST_DELAY = 1000 / MAX_REQUESTS_PER_SECOND;

    @Rule
    public final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"),
                    ConfigOverride.config("smtp.maxMailsPerSenderPerMinute", String.valueOf(1)),
                    ConfigOverride.config("rateLimiting.requestsPerMinute", String.valueOf(MAX_REQUESTS_PER_SECOND * 60)),
                    ConfigOverride.config("rateLimiting.burstLimit", String.valueOf(BURST_LENGTH)));

    @Test
    public void rateLimit_resourcesWithDifferentRateLimits() {

        var client = RULE.client();
        IntStream.rangeClosed(1, BURST_LENGTH + 1).forEach((i) -> {
            var response = client
                    .target(String.format("http://localhost:%d/api/my/profile", RULE.getLocalPort()))
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .get();

            // Expect failure after BURST_LENGTH requests
            assertThat(response.getStatus()).isEqualTo(i > BURST_LENGTH ? HttpStatus.TOO_MANY_REQUESTS_429 : HttpStatus.OK_200);
        });

        // Wait so that the overall rate limiter "forgets" about the previous burst of requests
        pause(REQUEST_DELAY);

        IntStream.rangeClosed(1, 3).forEach((i) -> {
            var response = client
                    .target(String.format("http://localhost:%d/api/my/send-set-password-link", RULE.getLocalPort()))
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .post(Entity.json(new ForgotPasswordDTO("alice@example.com")));

            // Expect failure after a single request since smtp.maxMailsPerSenderPerMinute = 1
            assertThat(response.getStatus()).isEqualTo(i > 1 ? HttpStatus.TOO_MANY_REQUESTS_429 : HttpStatus.NO_CONTENT_204);
        });
    }

    @Test
    public void rateLimit_slow() {
        var client = RULE.client();

        var response1 = client
                .target(String.format("http://localhost:%d/api/my/profile", RULE.getLocalPort()))
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK_200);

        pause(REQUEST_DELAY);

        var response2 = client
                .target(String.format("http://localhost:%d/api/my/profile", RULE.getLocalPort()))
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response2.getStatus()).isEqualTo(HttpStatus.OK_200);
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
