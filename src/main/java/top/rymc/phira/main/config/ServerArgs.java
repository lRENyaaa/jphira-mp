package top.rymc.phira.main.config;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import lombok.Getter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import top.rymc.phira.main.Server;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class ServerArgs {

    private static final Logger logger = Server.getLogger();

    private final int port;
    private final String host;
    private final Path pluginsDir;
    private final boolean proxyProtocol;
    private final String defaultLanguage;

    public ServerArgs(String[] args) {
        OptionParser parser = new OptionParser();

        OptionSpec<Integer> portSpec = parser.accepts("port", "Server listening port")
                .withRequiredArg()
                .ofType(Integer.class)
                .defaultsTo(12346);

        OptionSpec<String> hostSpec = parser.accepts("host", "Bind address (IP to listen on)")
                .withRequiredArg()
                .ofType(String.class)
                .defaultsTo("0.0.0.0");

        OptionSpec<String> pluginsSpec = parser.accepts("plugins", "Plugins directory path")
                .withRequiredArg()
                .ofType(String.class)
                .defaultsTo("plugins");

        OptionSpec<Boolean> proxyProtocol = parser.accepts("proxy-protocol", "Enable proxy protocol support")
                .withOptionalArg()
                .ofType(Boolean.class)
                .defaultsTo(false);

        OptionSpec<String> languageSpec = parser.accepts("language", "Default server language (e.g., zh-CN, en-US)")
                .withRequiredArg()
                .ofType(String.class)
                .defaultsTo("zh-CN");

        parser.accepts("help", "Show this help message").forHelp();

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (Exception e) {
            logger.error("Failed to parse arguments: {}", e.getMessage());
            printHelp(parser);
            LogManager.shutdown();
            System.exit(1);
            throw new AssertionError();
        }

        if (options.has("help")) {
            printHelp(parser);
            LogManager.shutdown();
            System.exit(0);
            throw new AssertionError();
        }

        this.port = clampPort(options.valueOf(portSpec));
        this.host = options.valueOf(hostSpec);
        this.pluginsDir = Paths.get(options.valueOf(pluginsSpec));
        this.proxyProtocol = options.valueOf(proxyProtocol);
        this.defaultLanguage = options.valueOf(languageSpec);
    }

    private void printHelp(OptionParser parser) {
        try {
            logger.info("Phira Server Usage:");
            PrintStream logStream = IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream();
            parser.printHelpOn(logStream);
        } catch (IOException e) {
            logger.error("Failed to print help: {}", e.getMessage());
        }
    }

    private static int clampPort(int value) {
        return Math.max(1, Math.min(value, 65535));
    }

}
