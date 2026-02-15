package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class RoomCreateEvent extends ReasonedCancellableEvent {

    private final Player creator;
    private final String roomId;
    private final Room.RoomSetting setting;

}
