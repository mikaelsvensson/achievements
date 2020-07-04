package se.devscout.achievements.server.health;

import com.codahale.metrics.health.HealthCheck;

public class IsAliveHealthcheck extends HealthCheck {
    protected Result check() {
        return Result.healthy();
    }
}
