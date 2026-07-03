package top.rymc.phira.main.game.room;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.game.room.state.RoomPlaying;
import top.rymc.phira.main.game.room.state.RoomSelectChart;
import top.rymc.phira.main.game.room.state.RoomWaitForReady;
import top.rymc.phira.main.util.ThreadFactoryCompat;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.data.state.WaitForReady;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJoinRoomPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProtocolHackService {

    public static final long CLIENT_STATE_DELAY_MILLIS = 100;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            ThreadFactoryCompat.THREAD_FACTORY_CREATOR.apply("ProtocolHackService")
    );

    public static Chain chain(Room room, Player player) {
        return new Chain(room, player);
    }

    public static RoomInfo buildRoomInfo(Room room, Player player) {
        RoomSnapshot snapshot = room.getView();
        RoomGameState state = snapshot.getState();
        ChartInfo chart = state.getChart();
        GameState protocolState = state.toProtocol();
        boolean ready = state instanceof RoomWaitForReady;
        boolean host = snapshot.isHost(player);

        if (state instanceof RoomWaitForReady) {
            if (chart != null) {
                protocolState = new SelectChart(chart.getId());
            }
            ready = false;
            if (host) {
                host = false;
            }
        } else if (state instanceof RoomPlaying) {
            protocolState = chart == null ? new SelectChart(null) : new SelectChart(chart.getId());
            ready = false;
        }

        return new RoomInfo(
                snapshot.getRoomId(),
                protocolState,
                snapshot.isLive(), snapshot.isLocked(), snapshot.isCycle(),
                host,
                ready,
                snapshot.getPlayers().stream().map(Player::toProtocol).toList(),
                snapshot.getMonitors().stream().map(Player::toProtocol).toList()
        );
    }

    public static ClientBoundJoinRoomPacket buildJoinSuccessPacket(Room room) {
        RoomSnapshot snapshot = room.getView();
        RoomGameState state = snapshot.getState();
        GameState protocolState;
        ChartInfo chart = state.getChart();
        if (!(state instanceof RoomSelectChart) && chart != null) {
            protocolState = new SelectChart(chart.getId());
        } else {
            protocolState = state.toProtocol();
        }

        return ClientBoundJoinRoomPacket.success(
                protocolState,
                snapshot.getPlayers().stream().map(Player::toProtocol).toList(),
                snapshot.getMonitors().stream().map(Player::toProtocol).toList(),
                snapshot.isLive()
        );
    }

    public static void reconnect(Room room, Player player) {
        RoomGameState state = room.getView().getState();
        if (state instanceof RoomWaitForReady) {
            Chain chain = chain(room, player);
            ChartInfo chart = state.getChart();
            if (chart != null) {
                chain.enterSelectChart(chart.getId())
                        .delay(CLIENT_STATE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                        .enterWaitForReady();
            } else {
                chain.enterWaitForReady();
            }

            chain.delay(CLIENT_STATE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .then("updateHost(true)", () -> {
                        if (room.isHost(player)) {
                            player.operations().ifPresent(operations -> operations.updateHostStatus(true));
                        }
                    });
            chain.submit();
        }
    }

    public static class Chain {
        private final Room room;
        private final Player player;
        private final Queue<Step> steps = new ConcurrentLinkedQueue<>();

        private Chain(Room room, Player player) {
            this.room = room;
            this.player = player;
        }

        public Chain then(Runnable action) {
            return then("custom", action);
        }

        public Chain then(String description, Runnable action) {
            steps.add(new Step("then", description, 0, action));
            return this;
        }

        public Chain delay(long timeout, TimeUnit unit) {
            steps.add(new Step("delay", timeout + " " + unit, unit.toMillis(timeout), () -> {}));
            return this;
        }

        public Chain enterSelectChart(Integer chartId) {
            return then("enterSelectChart(" + chartId + ")", () -> player.operations().ifPresent(operations -> operations.enterState(new SelectChart(chartId))));
        }

        public Chain enterWaitForReady() {
            return then("enterWaitForReady", () -> player.operations().ifPresent(operations -> operations.enterState(new WaitForReady())));
        }

        public Chain updateHost(boolean host) {
            return then("updateHost(" + host + ")", () -> player.operations().ifPresent(operations -> operations.updateHostStatus(host)));
        }

        public void submit() {
            executeNext();
        }

        private void executeNext() {
            Step step = steps.poll();
            if (step == null || !player.isOnline()) {
                return;
            }

            SCHEDULER.schedule(() -> {
                if (!player.isOnline()) {
                    return;
                }

                try {
                    step.action.run();
                    executeNext();
                } catch (Exception e) {
                    Server.getLogger().warn(
                            "Protocol hack step failed, player {}, room {}, step {}:{}",
                            player.getId(), room.getRoomId(), step.name, step.description, e
                    );
                }
            }, step.delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    private record Step(String name, String description, long delayMillis, Runnable action) {
    }
}
