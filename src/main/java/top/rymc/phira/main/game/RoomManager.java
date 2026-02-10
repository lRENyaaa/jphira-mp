package top.rymc.phira.main.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class RoomManager {
    private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();

    public static Room createRoom(String roomId, Room.RoomSetting setting) {
        return innerCreateRoom(roomId, id -> Room.create(id, key -> ROOMS.remove(roomId), setting));
    }

    public static Room createRoom(String roomId, Player host) {
        return innerCreateRoom(roomId, id -> Room.create(id, key -> ROOMS.remove(roomId), host));
    }

    private static Room innerCreateRoom(String roomId, Function<String, Room> creator) {
        if (ROOMS.containsKey(roomId)) {
            throw new IllegalStateException("Room already exists");
        }
        Room room = creator.apply(roomId);
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