package net.holm.iblockycompanion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.RegexFilter;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class LogFilter {

    private static final String LOGGER_NAME = "ClientCommandInternals";

    public static void applyLogFilter() throws IllegalAccessException {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig(LOGGER_NAME);

        // Create a RegexFilter to block the specific log message
        RegexFilter filter = RegexFilter.createFilter(
                ".*Ambiguity between arguments.*",
                null,
                true,
                RegexFilter.Result.DENY,
                RegexFilter.Result.NEUTRAL
        );

        // Add the filter to the logger configuration
        loggerConfig.addFilter(filter);
        ctx.updateLoggers();  // This makes sure the changes are applied
    }
}