package se.devscout.achievements.server;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import io.sentry.logback.SentryAppender;

/**
 * Slimmed-down version of SentryAppenderFactory from https://github.com/dhatim/dropwizard-sentry
 */
@JsonTypeName("sentry")
public class SentryAppenderFactory extends AbstractAppenderFactory<ILoggingEvent> {
    @Override
    public Appender<ILoggingEvent> build(LoggerContext loggerContext, String s, LayoutFactory<ILoggingEvent> layoutFactory, LevelFilterFactory<ILoggingEvent> levelFilterFactory, AsyncAppenderFactory<ILoggingEvent> asyncAppenderFactory) {
        final var appender = new SentryAppender();
        appender.setName("sentry-appender");
        appender.setContext(loggerContext);
        appender.addFilter(levelFilterFactory.build(threshold));
        getFilterFactories().forEach(f -> appender.addFilter(f.build()));
        appender.start();

        final var filter = new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent iLoggingEvent) {
                if (iLoggingEvent.getLoggerName().startsWith("io.sentry")) {
                    return FilterReply.DENY;
                } else {
                    return FilterReply.NEUTRAL;
                }
            }
        };
        filter.start();
        appender.addFilter(filter);

        return wrapAsync(appender, asyncAppenderFactory, loggerContext);
    }
}
