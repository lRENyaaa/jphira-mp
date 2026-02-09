package top.rymc.phira.main.command;

import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.CommandProcessEvent;

@RequiredArgsConstructor
public class CommandService extends SimpleTerminalConsole {

    private final Thread thread = new Thread(super::start);

    private final Logger logger;

    @Override
    public boolean isRunning() {
        return Server.getInstance().isRunning();
    }

    @Override
    public void runCommand(String command) {
        if (command.trim().equalsIgnoreCase("stop")) {
            Server.getInstance().shutdown();
            return;
        }

        CommandProcessEvent event = new CommandProcessEvent(command);
        if (!Server.postEvent(event)) {
            logger.warn("Unknown command: {}", command);
        }
    }

    @Override
    public void shutdown() {
        Server.getInstance().shutdown();
    }

    @Override
    public void start() {
        thread.start();
    }

}
