package top.rymc.phira.main.command;

import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.PlayerManager;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.main.game.room.RoomSnapshot;
import top.rymc.phira.main.game.room.chart.ChartPool;
import top.rymc.phira.main.game.room.chart.RoomChartPool;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.main.game.room.local.LocalRoomBuilder;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.game.room.state.RoomPlaying;
import top.rymc.phira.main.game.room.state.RoomSelectChart;
import top.rymc.phira.main.game.room.state.RoomWaitForReady;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class CommandService extends SimpleTerminalConsole {

    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("[A-Za-z_-]{1,20}");
    private static final List<String> CONFIG_KEYS = List.of(
            "minPlayer", "maxPlayer", "selectCountdown", "readyCountdown", "forceFinish", "interval"
    );

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

        if (commandName.toLowerCase().startsWith("room ")) {
            handleRoomCommand(commandName.split("\\s+"));
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

    private void handleRoomCommand(String[] args) {
        try {
            if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
                listRooms();
                return;
            }

            if (args.length >= 3 && args[1].equalsIgnoreCase("create")) {
                createRoom(args);
                return;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                removeRoom(args[2]);
                return;
            }

            if (args.length >= 3) {
                String roomId = args[1];
                if (args[2].equalsIgnoreCase("end")) {
                    forceEndRoom(roomId);
                    return;
                }
                if (args[2].equalsIgnoreCase("config")) {
                    if (args.length == 5) {
                        setRoomConfig(roomId, args[3], args[4]);
                        return;
                    }
                }
                if (args[2].equalsIgnoreCase("pool")) {
                    if (args.length == 5 && args[3].equalsIgnoreCase("switch")) {
                        switchRoomPool(roomId, parseInt(args[4]));
                        return;
                    }
                    if (args.length == 5 && args[3].equalsIgnoreCase("favorite")) {
                        setRoomPoolFavorite(roomId, args[4]);
                        return;
                    }
                }
            }

            logger.warn("Usage: room list | room create <id> [poolId...] | room remove <id> | room <id> end | room <id> config <key> <value> | room <id> pool switch <poolId> | room <id> pool favorite <favoriteId|none>");
        } catch (Exception e) {
            logger.warn("Room command failed: {}", e.getMessage());
        }
    }

    private void listRooms() {
        List<Room> rooms = RoomManager.getAllRooms();
        if (rooms.isEmpty()) {
            logger.info("No rooms");
            return;
        }

        for (Room room : rooms) {
            RoomSnapshot view = room.getView();
            RoomGameState state = view.getState();
            long online = view.getPlayers().stream().filter(p -> p.isOnline()).count();
            LocalRoom localRoom = (LocalRoom) room;
            RoomChartPool chartPool = localRoom.getChartPool();
            String phase = state instanceof RoomPlaying ? "Playing"
                    : state instanceof RoomWaitForReady ? "WaitForReady" : "SelectChart";
            String chartName = state.getChart() == null ? "-" : state.getChart().getName();
            logger.info("Room {} [{}] chart={} players={}/{} monitors={} pool={} rounds={}/{}",
                    view.getRoomId(), phase, chartName, online, view.getPlayers().size(), view.getMonitors().size(),
                    chartPool.getCurrentPoolSnapshot().id(),
                    chartPool.getFinishedRoundsSinceRefresh(),
                    localRoom.getSetting().getRefreshIntervalRounds());
        }
    }

    private void createRoom(String[] args) {
        String roomId = args[2];
        if (!ROOM_ID_PATTERN.matcher(roomId).matches()) {
            throw new IllegalArgumentException("Room id must be letters, digits, - or _ with length <= 20");
        }
        if (RoomManager.findRoom(roomId) != null) {
            throw new IllegalArgumentException("Room already exists: " + roomId);
        }

        List<ChartPool.PoolSnapshot> pools;
        if (args.length > 3) {
            pools = new ArrayList<>();
            for (int i = 3; i < args.length; i++) {
                int poolId = parseInt(args[i]);
                ChartPool.PoolSnapshot pool = ChartPool.findPool(poolId);
                if (pool == null) {
                    throw new IllegalArgumentException("Pool not found: " + poolId);
                }
                pools.add(pool);
            }
        } else {
            pools = ChartPool.getDefaultPools();
            if (pools.isEmpty()) {
                throw new IllegalArgumentException("No default pools available");
            }
        }

        new LocalRoomBuilder()
                .host(false)
                .cycle(false)
                .chat(true)
                .autoDestroy(false)
                .pools(pools)
                .build(roomId);
        logger.info("Created room {}", roomId);
    }

    private void removeRoom(String roomId) {
        Room room = RoomManager.findRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        RoomSnapshot view = room.getView();
        if (!view.getPlayers().isEmpty() || !view.getMonitors().isEmpty()) {
            throw new IllegalArgumentException("Room is not empty, cannot remove");
        }

        RoomManager.removeRoom(roomId);
        logger.info("Removed room {}", roomId);
    }

    private void forceEndRoom(String roomId) {
        Room room = requireRoom(roomId);
        if (!(room.getView().getState() instanceof RoomPlaying state)) {
            throw new IllegalArgumentException("Room is not playing");
        }
        state.forceFinishByServer();
        logger.info("Force ended current game in room {}", roomId);
    }

    private void setRoomConfig(String roomId, String key, String value) {
        LocalRoom room = requireLocalRoom(roomId);
        LocalRoom.RoomSetting setting = room.getSetting();
        int intValue = parseInt(value);

        switch (key.toLowerCase()) {
            case "minplayer" -> {
                if (intValue <= 0) {
                    throw new IllegalArgumentException("minPlayer must be positive");
                }
                setting.setMinPlayer(intValue);
            }
            case "maxplayer" -> {
                if (intValue <= 0) {
                    throw new IllegalArgumentException("maxPlayer must be positive");
                }
                setting.setMaxPlayer(intValue);
            }
            case "selectcountdown" -> {
                if (intValue < 10) {
                    throw new IllegalArgumentException("selectCountdown must be at least 10");
                }
                setting.setSelectChartCountdownSeconds(intValue);
            }
            case "readycountdown" -> {
                if (intValue <= 0) {
                    throw new IllegalArgumentException("readyCountdown must be positive");
                }
                setting.setReadyCountdownSeconds(intValue);
            }
            case "forcefinish" -> {
                if (intValue <= 0) {
                    throw new IllegalArgumentException("forceFinish must be positive");
                }
                setting.setForceFinishSeconds(intValue);
            }
            case "interval" -> {
                if (intValue <= 0) {
                    throw new IllegalArgumentException("interval must be positive");
                }
                setting.setRefreshIntervalRounds(intValue);
            }
            default -> throw new IllegalArgumentException(
                    "Unknown config key: " + key + ". Keys: " + String.join(", ", CONFIG_KEYS));
        }
        logger.info("Room {} config {} = {}", roomId, key, value);
    }

    private void switchRoomPool(String roomId, int poolId) {
        LocalRoom room = requireLocalRoom(roomId);
        room.getChartPool().switchPool(poolId);
        logger.info("Room {} pending pool set to {}", roomId, poolId);
    }

    private void setRoomPoolFavorite(String roomId, String value) {
        LocalRoom room = requireLocalRoom(roomId);
        Integer favoriteId = value.equalsIgnoreCase("none") ? null : parseInt(value);
        room.getChartPool().setFavorite(favoriteId);
        if (favoriteId == null) {
            logger.info("Cleared favorite_id for room {} current pool", roomId);
        } else {
            logger.info("Set favorite_id for room {} current pool to {}", roomId, favoriteId);
        }
    }

    private void handlePoolCommand(String[] args) {
        try {
            if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
                listPools();
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

            if (args.length == 4 && args[1].equalsIgnoreCase("favorite")) {
                setPoolFavorite(parseInt(args[2]), args[3]);
                return;
            }

            if (args.length == 4 && args[1].equalsIgnoreCase("default")) {
                setPoolDefaultFlag(parseInt(args[2]), args[3]);
                return;
            }

            logger.warn("Usage: pool list | pool add <id> <chartIds...> | pool remove <id> | pool chart add <poolId> <chartId> | pool chart remove <poolId> <chartId> | pool favorite <poolId> <favoriteId|none> | pool default <poolId> <on|off>");
        } catch (Exception e) {
            logger.warn("Pool command failed: {}", e.getMessage());
        }
    }

    private void listPools() {
        for (ChartPool.PoolSnapshot pool : ChartPool.listPools()) {
            if (pool.defaultFlag()) {
                logger.info("Pool {} default favorite_id={}:", pool.id(), pool.favoriteId());
            } else {
                logger.info("Pool {} favorite_id={}:", pool.id(), pool.favoriteId());
            }
            for (int chartId : pool.chartIds()) {
                ChartInfo chart = ChartPool.getChartInfo(chartId);
                logger.info("  {} | Lv.{} | ID:{}", chart.getName(), chart.getLevel(), chart.getId());
            }
        }
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

    private void setPoolFavorite(int poolId, String value) {
        Integer favoriteId = value.equalsIgnoreCase("none") ? null : parseInt(value);
        ChartPool.setFavoriteId(poolId, favoriteId);
        if (favoriteId == null) {
            logger.info("Cleared favorite_id for pool {}", poolId);
        } else {
            logger.info("Set favorite_id for pool {} to {}", poolId, favoriteId);
        }
    }

    private void setPoolDefaultFlag(int poolId, String value) {
        boolean flag = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true") || value.equals("1");
        ChartPool.setDefaultFlag(poolId, flag);
        logger.info("Set default flag for pool {} to {}", poolId, flag);
    }

    private List<Integer> parseChartIds(String[] args) {
        return Arrays.stream(args)
                .map(this::parseInt)
                .toList();
    }

    private int parseInt(String value) {
        return Integer.parseInt(value);
    }

    private LocalRoom requireLocalRoom(String roomId) {
        Room room = requireRoom(roomId);
        return (LocalRoom) room;
    }

    private Room requireRoom(String roomId) {
        Room room = RoomManager.findRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        return room;
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
