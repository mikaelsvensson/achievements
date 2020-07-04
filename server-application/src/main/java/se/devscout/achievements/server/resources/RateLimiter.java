package se.devscout.achievements.server.resources;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class RateLimiter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiter.class);

    private static class Record {
        private long earliestNextRequest;
        private long backoff;
        private long burstLimit;

        Record(long earliestNextRequest, long backoff, long burstLimit) {
            this.earliestNextRequest = earliestNextRequest;
            this.backoff = backoff;
            this.burstLimit = burstLimit;
        }
    }

    private final Cache<String, Record> cache = CacheBuilder.newBuilder().maximumSize(1000).build();

    private final long minimumMillisecondsBetweenRequests;

    private final long defaultBurstLimit;

    public RateLimiter(int requestsPerMinute, int defaultBurstLimit) {
        this.minimumMillisecondsBetweenRequests = Duration.ofMinutes(1).toMillis() / requestsPerMinute;
        this.defaultBurstLimit = defaultBurstLimit;
    }

    public boolean accept(String client) {
        final var now = System.currentTimeMillis();
        final var record = cache.getIfPresent(client);
        if (record == null) {
            cache.put(client, new Record(
                    now + minimumMillisecondsBetweenRequests,
                    minimumMillisecondsBetweenRequests,
                    defaultBurstLimit - 1));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(client + " is not currently being monitored. Starting request counter.");
            }
        } else {
            if (record.earliestNextRequest < now) {
                cache.invalidate(client);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(client + " was monitored but is not anymore.");
                }
            } else {
                record.earliestNextRequest = now + record.backoff;
                if (record.burstLimit > 0) {
                    record.burstLimit--;
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(client + " can make " + record.burstLimit + " more request before the rate limit kicks in.");
                    }
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(client + " is not allowed to perform request because of rate limit.");
                    }
                    return false;
                }
            }
        }
        return true;
    }
}
