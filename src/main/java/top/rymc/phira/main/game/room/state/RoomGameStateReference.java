package top.rymc.phira.main.game.room.state;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class RoomGameStateReference {

    private final AtomicReference<RoomGameState> stateReference = new AtomicReference<>();

    public RoomGameStateReference(Function<Consumer<RoomGameState>,RoomGameState> stateBuilder) {
        stateReference.set(stateBuilder.apply(stateReference::set));
    }

    public RoomGameState get() {
        return stateReference.get();
    }
}
