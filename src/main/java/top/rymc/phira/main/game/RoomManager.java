package top.rymc.phira.main.game;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private static final Map<String, Room> ROOM_MAP = new ConcurrentHashMap<>();

    public static Room createRoom(String roomId, @Nullable Player defaultPlayer) {
        if (ROOM_MAP.get(roomId) != null) {
            throw new RuntimeException("Duplicate create"); // TODO throw right exception
        }

        Room room = Room.create(roomId, (r) -> ROOM_MAP.remove(r.getRoomId()), defaultPlayer);
        ROOM_MAP.put(roomId, room);
        return room;

    }

    public static Room findRoom(String roomId) {
        return ROOM_MAP.get(roomId);
    }
}
