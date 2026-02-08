package top.rymc.phira.main.game;

import java.util.ArrayList;
import java.util.List;
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

    public static List<Room> getAllRooms() {
        return new ArrayList<>(ROOMS.values());
    }
}