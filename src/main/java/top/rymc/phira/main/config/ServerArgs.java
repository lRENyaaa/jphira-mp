package top.rymc.phira.main.config;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class ServerArgs {
    private final int port;
    private final String host;
    private final Path pluginsDir;
    private final boolean proxyProtocol;

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

        parser.accepts("help", "Show this help message").forHelp();

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (Exception e) {
            System.err.println("Failed to parse arguments: " + e.getMessage());
            printHelp(parser);
            System.exit(1);
            throw new AssertionError();
        }

        if (options.has("help")) {
            printHelp(parser);
            System.exit(0);
        }

        this.port = options.valueOf(portSpec);
        this.host = options.valueOf(hostSpec);
        this.pluginsDir = Paths.get(options.valueOf(pluginsSpec));
        this.proxyProtocol = options.valueOf(proxyProtocol);
    }

    private void printHelp(OptionParser parser) {
        try {
            System.out.println("Phira Server Usage:");
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            System.err.println("Failed to print help: " + e.getMessage());
        }
    }
}
