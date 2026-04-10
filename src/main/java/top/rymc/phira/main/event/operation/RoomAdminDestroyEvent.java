package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@Getter
@RequiredArgsConstructor
public class RoomAdminDestroyEvent extends ReasonedCancellableEvent {

    private final LocalRoom room;

}
