package se.devscout.achievements.server.filter.audit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    boolean logRequest() default false;
}
