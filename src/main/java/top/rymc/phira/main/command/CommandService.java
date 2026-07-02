package top.rymc.phira.main.command;

import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.PlayerManager;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.main.game.room.chart.ChartPool;
import top.rymc.phira.main.game.room.state.RoomPlaying;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

        if (commandName.toLowerCase().startsWith("pool ")) {
            handlePoolCommand(commandName.split("\\s+"));
            return;
        }

        if (commandName.toLowerCase().startsWith("say ")) {
            String trim = commandName.substring(4).trim();
            PlayerManager.getOnlinePlayers().forEach(player -> player.operations().ifPresent(op -> op.receiveChat(-1, trim)));
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

    private void handlePoolCommand(String[] args) {
        try {
            if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
                listPools();
                return;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("switch")) {
                switchPool(parseInt(args[2]));
                return;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("interval")) {
                ChartPool.setRefreshInterval(parseInt(args[2]));
                logger.info("Set chart pool refresh interval to {} round(s)", args[2]);
                return;
            }

            if (args.length == 4 && args[1].equalsIgnoreCase("favorite")) {
                setFavorite(parseInt(args[2]), args[3]);
                return;
            }

            if (args.length == 4 && args[1].equalsIgnoreCase("current") && args[2].equalsIgnoreCase("favorite")) {
                setCurrentFavorite(args[3]);
                return;
            }

            if (args.length >= 4 && args[1].equalsIgnoreCase("add")) {
                addPool(parseInt(args[2]), parseChartIds(Arrays.copyOfRange(args, 3, args.length)));
                return;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                ChartPool.removePool(parseInt(args[2]));
                logger.info("Removed chart pool {}", args[2]);
                return;
            }

            if (args.length == 5 && args[1].equalsIgnoreCase("chart") && args[2].equalsIgnoreCase("add")) {
                addChart(parseInt(args[3]), parseInt(args[4]));
                return;
            }

            if (args.length == 5 && args[1].equalsIgnoreCase("chart") && args[2].equalsIgnoreCase("remove")) {
                ChartPool.removeChart(parseInt(args[3]), parseInt(args[4]));
                logger.info("Removed chart {} from pool {}", args[4], args[3]);
                return;
            }

            logger.warn("Usage: pool list | pool switch <id> | pool interval <rounds> | pool favorite <poolId> <favoriteId|none> | pool current favorite <favoriteId|none> | pool add <id> <chartIds...> | pool remove <id> | pool chart add <poolId> <chartId> | pool chart remove <poolId> <chartId>");
        } catch (Exception e) {
            logger.warn("Pool command failed: {}", e.getMessage());
        }
    }

    private void listPools() {
        ChartPool.PoolStatus status = ChartPool.getStatus();
        logger.info("Current pool: {}, pending pool: {}, refresh: {}/{} round(s)",
                status.currentPoolId(), status.pendingPoolId(), status.finishedRoundsSinceRefresh(), status.refreshIntervalRounds());
        for (ChartPool.PoolSnapshot pool : ChartPool.listPools()) {
            if (pool.favoriteId() == null) {
                logger.info("Pool {}:", pool.id());
            } else {
                logger.info("Pool {} favorite_id={}:", pool.id(), pool.favoriteId());
            }
            for (int chartId : pool.chartIds()) {
                ChartInfo chart = ChartPool.getChartInfo(chartId);
                logger.info("  {} | Lv.{} | ID:{}", chart.getName(), chart.getLevel(), chart.getId());
            }
        }
    }

    private void switchPool(int poolId) {
        ChartPool.switchPool(poolId);
        logger.info("Set pending chart pool to {}. It will take effect on next pool refresh.", poolId);
    }

    private void addPool(int poolId, List<Integer> chartIds) throws IOException {
        ChartPool.addPool(poolId, chartIds);
        logger.info("Added chart pool {} with {} chart(s)", poolId, chartIds.size());
    }

    private void addChart(int poolId, int chartId) throws IOException {
        ChartPool.addChart(poolId, chartId);
        ChartInfo chart = ChartPool.getChartInfo(chartId);
        logger.info("Added chart to pool {}: {} | Lv.{} | ID:{}", poolId, chart.getName(), chart.getLevel(), chart.getId());
    }

    private void setFavorite(int poolId, String value) {
        Integer favoriteId = value.equalsIgnoreCase("none") ? null : parseInt(value);
        ChartPool.setFavoriteId(poolId, favoriteId);
        if (favoriteId == null) {
            logger.info("Cleared favorite_id for pool {}", poolId);
        } else {
            logger.info("Set favorite_id for pool {} to {}", poolId, favoriteId);
        }
    }

    private void setCurrentFavorite(String value) {
        Integer favoriteId = value.equalsIgnoreCase("none") ? null : parseInt(value);
        ChartPool.setCurrentFavoriteId(favoriteId);
        if (favoriteId == null) {
            logger.info("Cleared favorite_id for current pool");
        } else {
            logger.info("Set favorite_id for current pool to {}", favoriteId);
        }
    }

    private List<Integer> parseChartIds(String[] args) {
        return Arrays.stream(args)
                .map(this::parseInt)
                .toList();
    }

    private int parseInt(String value) {
        return Integer.parseInt(value);
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
