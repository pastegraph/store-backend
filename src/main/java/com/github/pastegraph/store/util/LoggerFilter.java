package com.github.pastegraph.store.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class LoggerFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        String levelFromEnv = System.getenv("LOG_LEVEL");
        if (levelFromEnv == null) levelFromEnv = "INFO";
        if (iLoggingEvent.getLevel().toInt() >= Level.toLevel(levelFromEnv).toInt())
            return FilterReply.ACCEPT;
        else return FilterReply.DENY;
    }
}
