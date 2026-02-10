package top.rymc.phira.logger.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PerformanceSensitive;

import java.util.List;
import java.util.regex.Pattern;

@ConverterKeys({"stripAnsi"})
@Plugin(name = "StripAnsiConverter", category = PatternConverter.CATEGORY)
public final class AnsiStrippingPatternConverter extends LogEventPatternConverter {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[;0-9]*m");
    private final List<PatternFormatter> formatters;

    private AnsiStrippingPatternConverter(List<PatternFormatter> formatters) {
        super("stripAnsi", "stripAnsi");
        this.formatters = formatters;
    }

    public static AnsiStrippingPatternConverter newInstance(Configuration config, String[] options) {
        if (options == null || options.length != 1 || config == null) {
            LOGGER.error("stripAnsi requires exactly one pattern parameter and valid configuration");
            return null;
        }
        try {
            PatternParser parser = PatternLayout.createPatternParser(config);
            return new AnsiStrippingPatternConverter(parser.parse(options[0]));
        } catch (Exception e) {
            LOGGER.error("Failed to parse pattern: " + options[0], e);
            return null;
        }
    }

    @Override
    @PerformanceSensitive("allocation")
    public void format(LogEvent event, StringBuilder toAppendTo) {
        int start = toAppendTo.length();
        for (PatternFormatter formatter : formatters) {
            formatter.format(event, toAppendTo);
        }
        if (toAppendTo.length() > start) {
            String cleaned = ANSI_PATTERN.matcher(toAppendTo.substring(start)).replaceAll("");
            toAppendTo.setLength(start);
            toAppendTo.append(cleaned);
        }
    }

    @Override
    public boolean handlesThrowable() {
        for (PatternFormatter formatter : formatters) {
            if (formatter.handlesThrowable()) return true;
        }
        return false;
    }
}