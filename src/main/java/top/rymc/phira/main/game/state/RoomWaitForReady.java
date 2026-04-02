package top.rymc.phira.main.game.state;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.event.game.PlayerReadyEvent;
import top.rymc.phira.main.event.game.GamePlayingStartEvent;
import top.rymc.phira.main.event.game.PlayerCancelReadyEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.protocol.data.message.CancelReadyMessage;
import top.rymc.phira.protocol.data.message.ReadyMessage;
import top.rymc.phira.protocol.data.message.StartPlayingMessage;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.WaitForReady;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RoomWaitForReady extends RoomGameState {

    private final Set<LocalPlayer> readyPlayers = ConcurrentHashMap.newKeySet();

    public RoomWaitForReady(Room room, Consumer<RoomGameState> stateUpdater) {
        super(room, stateUpdater);
    }

    public RoomWaitForReady(Room room, Consumer<RoomGameState> stateUpdater, ChartInfo chart, LocalPlayer initiator) {
        this(room, stateUpdater, chart);
        readyPlayers.add(initiator);
    }

    public RoomWaitForReady(Room room, Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        super(room, stateUpdater, chart);
    }

    @Override
    public void handleJoin(LocalPlayer player) {

    }

    @Override
    public void handleLeave(LocalPlayer player) {
        readyPlayers.remove(player);
    }

    @Override
    public void requireStart(LocalPlayer player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void ready(LocalPlayer player) {
        readyPlayers.add(player);
        broadcast(ClientBoundMessagePacket.create(new ReadyMessage(player.getId())));
        updateState();

        PlayerReadyEvent event = new PlayerReadyEvent(player, room);
        Server.postEvent(event);
    }

    @Override
    public void cancelReady(LocalPlayer player) {
        readyPlayers.remove(player);
        broadcast(ClientBoundMessagePacket.create(new CancelReadyMessage(player.getId())));

        PlayerCancelReadyEvent event = new PlayerCancelReadyEvent(player, room);
        Server.postEvent(event);
    }

    @Override
    public void touchSend(LocalPlayer player, List<TouchFrame> touchFrames) {

    }

    @Override
    public void judgeSend(LocalPlayer player, List<JudgeEvent> judgeEvents) {

    }

    @Override
    public void abort(LocalPlayer player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void played(LocalPlayer player, int recordId) {
        throw GameOperationException.invalidState();
    }

    private void updateState() {
        if (isAllOnlinePlayersDone()) {
            Set<LocalPlayer> players = room.getPlayers();
            Set<LocalPlayer> monitors = room.getMonitors();
            GamePlayingStartEvent event = new GamePlayingStartEvent(room, chart, Set.copyOf(players), Set.copyOf(monitors));
            Server.postEvent(event);

            RoomPlaying state = new RoomPlaying(room, stateUpdater, chart);
            updateGameState(state);
            broadcast(ClientBoundMessagePacket.create(StartPlayingMessage.INSTANCE));
        }

    }

    private boolean isAllOnlinePlayersDone() {
        Set<LocalPlayer> allPlayers = new HashSet<>(room.getPlayers());
        allPlayers.addAll(room.getMonitors());

        Set<LocalPlayer> onlinePlayers = allPlayers.stream()
                .filter(LocalPlayer::isOnline)
                .collect(Collectors.toSet());

        return readyPlayers.containsAll(onlinePlayers);
    }

    @Override
    public GameState toProtocol() {
        return new WaitForReady();
    }
}
