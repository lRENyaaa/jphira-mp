package top.rymc.phira.main.game.room;

import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class RoomManager {
    private static final Map<String, LocalRoom> ROOMS = new ConcurrentHashMap<>();

    public static LocalRoom createRoom(String roomId, LocalPlayer host, LocalRoom.RoomSetting setting) {
        return innerCreateRoom(roomId, id -> LocalRoom.create(id, key -> ROOMS.remove(roomId), host, setting));
    }

    private static LocalRoom innerCreateRoom(String roomId, Function<String, LocalRoom> creator) {
        if (ROOMS.containsKey(roomId)) {
            throw GameOperationException.roomAlreadyExists();
        }
        LocalRoom room = creator.apply(roomId);
        ROOMS.put(roomId, room);
        return room;
    }

    public static LocalRoom findRoom(String roomId) {
        return ROOMS.get(roomId);
    }

    public static List<LocalRoom> getAllRooms() {
        return new ArrayList<>(ROOMS.values());
    }
}