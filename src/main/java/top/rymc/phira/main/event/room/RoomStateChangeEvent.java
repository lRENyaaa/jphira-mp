package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class RoomStateChangeEvent extends Event {
    private final RoomGameState oldState;
    private final RoomGameState newState;
}
