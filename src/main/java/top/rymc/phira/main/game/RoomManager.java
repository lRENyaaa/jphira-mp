package top.rymc.phira.main.game;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();

    public static Room createRoom(String roomId, Player host) {
        if (ROOMS.containsKey(roomId)) {
            throw new IllegalStateException("Room already exists");
        }
        Room room = Room.create(roomId, key -> ROOMS.remove(roomId), host);
        ROOMS.put(roomId, room);
        return room;
    }

    public static Room findRoom(String roomId) {
        return ROOMS.get(roomId);
    }

    /** 当前本机所有房间（用于关闭时清理 Redis）。 */
    public static Collection<Room> getAllRooms() {
        return ROOMS.values();
    }
}