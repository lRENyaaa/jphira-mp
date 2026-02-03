package top.rymc.phira.main.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.RoomHandler;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.UserProfile;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class Player {

    @Getter
    private final UserInfo userInfo;
    @Getter
    private PlayerConnection connection;
    @Getter
    private Room room;
    private Consumer<Player> removeFromRoom;

    private final Consumer<Player> removeFromManager;

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timeout;

    public static Player create(UserInfo userInfo, PlayerConnection connection, Consumer<Player> removeFromManager) {
        Player player = new Player(userInfo, removeFromManager);
        player.bind(connection);
        return player;
    }

    protected void bind(PlayerConnection connection) {
        this.connection = connection;
        if (timeout != null) timeout.cancel(false);
        connection.onClose((ctx) -> {
            if (room == null) {
                kick();
            } else {
                timeout = TIMER.schedule(this::kick, 300, TimeUnit.SECONDS);
            }
        });
    }

    protected boolean joinRoom(Room room, Consumer<Player> removeFromRoom) {
        boolean result = this.room == null;
        if (result) {
            this.room = room;
            this.removeFromRoom = removeFromRoom;
            connection.setPacketHandler(new RoomHandler(this, room, connection.getPacketHandler()));
        }
        return result;
    }

    public void kick() {
        room = null;
        if (removeFromRoom != null) removeFromRoom.accept(this);
        removeFromManager.accept(this);
        if (isOnline()) connection.close();
    }


    public boolean isOnline() {
        return !connection.isClosed();
    }

    public RoomInfo getRoomInfo() {
        return room == null ? null : room.toRoomInfo(this);
    }

    public int getId() {
        return userInfo.getId();
    }

    public String getName() {
        return userInfo.getName();
    }

    public UserProfile getUserProfile() {
        return new UserProfile(userInfo.getId(), userInfo.getName());
    }


}
