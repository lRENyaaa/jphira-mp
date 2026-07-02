package top.rymc.phira.main.command;

import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.main.game.room.state.RoomPlaying;

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
        String commandName = command.trim();
        if (commandName.equalsIgnoreCase("stop")) {
            Server.getInstance().shutdown();
            return;
        }

        if (commandName.equalsIgnoreCase("end") || commandName.equalsIgnoreCase("forceend")) {
            forceEndGame();
            return;
        }

        logger.warn("Unknown command: {}", command);
    }

    private void forceEndGame() {
        Room room = RoomManager.findRoom("zenith");
        if (room == null) {
            logger.warn("Room zenith not found");
            return;
        }

        if (!(room.getView().getState() instanceof RoomPlaying state)) {
            logger.warn("Room zenith is not playing");
            return;
        }

        state.forceFinishByServer();
        logger.info("Force ended current game in room zenith");
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
