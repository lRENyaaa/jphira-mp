package top.rymc.phira.main.game.room.state;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.room.RoomStateChangeEvent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class RoomGameStateReference {

    private final AtomicReference<RoomGameState> stateReference = new AtomicReference<>();

    public RoomGameStateReference(Function<Consumer<RoomGameState>, RoomGameState> stateBuilder) {
        stateReference.set(stateBuilder.apply(this::updateState));
    }

    private void updateState(RoomGameState newState) {
        RoomGameState oldState = stateReference.get();
        stateReference.set(newState);

        RoomStateChangeEvent event = new RoomStateChangeEvent(oldState, newState);
        Server.postEvent(event);
    }

    public RoomGameState get() {
        return stateReference.get();
    }
}
