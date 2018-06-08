package se.devscout.achievements.server.resources;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

public class RateLimitFilterCoarseGrained implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilterCoarseGrained.class);
    private final long minimumMillisecondsBetweenRequests;
    private final long defaultRequestBurstCount;
    private Cache<String, Record> cache = CacheBuilder.newBuilder().maximumSize(1000).build();

    private static class Record {
        private long earliestNextRequest;
        private long backoff;
        private long grace;

        public Record(long earliestNextRequest, long backoff, long grace) {
            this.earliestNextRequest = earliestNextRequest;
            this.backoff = backoff;
            this.grace = grace;
        }
    }

    public RateLimitFilterCoarseGrained(int requestsPerMinute, int defaultRequestBurstCount) {
        this.minimumMillisecondsBetweenRequests = Duration.ofMinutes(1).toMillis() / requestsPerMinute;
        this.defaultRequestBurstCount = defaultRequestBurstCount;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            if (request.getMethod().equals("OPTIONS") || request.getMethod().equals("HEAD")) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }
        boolean isTooEarly = false;

        final long now = System.currentTimeMillis();
        final String ip = servletRequest.getRemoteAddr();
        final Record record = cache.getIfPresent(ip);
        if (record == null) {
            cache.put(ip, new Record(
                    now + minimumMillisecondsBetweenRequests,
                    minimumMillisecondsBetweenRequests,
                    defaultRequestBurstCount - 1));
            isTooEarly = false;
            LOGGER.debug(ip + " is not currently being monitored. Starting request counter.");
        } else {
            if (record.earliestNextRequest < now) {
                cache.invalidate(ip);
                LOGGER.debug(ip + " was monitored but is not anymore.");
            } else {
                record.earliestNextRequest = now + record.backoff;
                if (record.grace > 0) {
                    record.grace--;
                    LOGGER.debug(ip + " can make " + record.grace + " more request before the rate limit kicks in.");
                } else {
                    LOGGER.info(ip + " is not allowed to perform request because of rate limit.");
                    isTooEarly = true;
                }
            }
        }


        if (isTooEarly) {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(429);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
        cache.cleanUp();
    }
}
