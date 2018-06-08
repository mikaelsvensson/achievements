package se.devscout.achievements.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    int requestsPerMinute();

    int burstLimit();
}
