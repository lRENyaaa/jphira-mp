package top.rymc.phira.main.game.room;

import top.rymc.phira.main.game.exception.GameOperationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class RoomManager {
    private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();

    public static <T extends Room> T resolveRoom(String roomId, Function<Runnable, T> constructor) {

        AtomicReference<T> reference = new AtomicReference<>();
        ROOMS.compute(roomId, (id, exist) -> {
            if (exist != null) {
                throw GameOperationException.roomAlreadyExists();
            }

            T room = constructor.apply(() -> ROOMS.remove(roomId));
            reference.set(room);
            return room;
        });
        T room = reference.get();
        if (room == null) {
            throw new AssertionError();
        }

        return room;
    }

    public static Room findRoom(String roomId) {
        return ROOMS.get(roomId);
    }

    public static List<Room> getAllRooms() {
        return new ArrayList<>(ROOMS.values());
    }
}