package top.rymc.phira.main.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.state.GameState;
import top.rymc.phira.main.game.state.SelectChart;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.UserProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class Room {

    private GameState state = new SelectChart((state) -> this.state = state);

    @Getter
    private final String roomId;
    private final Consumer<Room> removeFromManager;

    private boolean live = false;
    private boolean locked = false;
    private boolean cycle = false;

    private Player host;

    private final List<Player> players = new ArrayList<>();

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    private final List<Player> monitors = new ArrayList<>();

    public List<Player> getMonitors() {
        return new ArrayList<>(monitors);
    }

    public static Room create(String roomId, Consumer<Room> removeFromManager, Player defaultPlayer) {
        Room room = new Room(roomId, removeFromManager);
        room.handleJoin(defaultPlayer, false);
        room.host = defaultPlayer;
        return room;
    }

    public void handleJoin(Player player, boolean isMonitor) {
        if (!player.joinRoom(this, this::handleLeave)) return;
        List<Player> list = isMonitor ? monitors : players;
        list.add(player);
        state.handleJoin(player);
    }

    private void handleLeave(Player player) {
        players.remove(player);
        monitors.remove(player);
        state.handleLeave(player);
        if (players.isEmpty()) removeFromManager.accept(this);
    }

    private boolean isHost(Player player) {
        return host != null && host.getId() == player.getId();
    }

    public RoomInfo toRoomInfo(Player player) {
        boolean isHost = player.equals(host);
        List<UserProfile> players = this.players.stream().map(Player::getUserProfile).toList();
        List<UserProfile> monitors = this.monitors.stream().map((Player::getUserProfile)).toList();
        top.rymc.phira.protocol.data.state.GameState gameState = state.toProtocolGameState();
        boolean isReady = gameState instanceof top.rymc.phira.protocol.data.state.WaitForReady;
        return new RoomInfo(roomId, gameState, live, locked, cycle, isHost, isReady, players, monitors);
    }
}
