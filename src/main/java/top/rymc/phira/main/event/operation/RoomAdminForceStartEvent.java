package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@Getter
@RequiredArgsConstructor
public class RoomAdminForceStartEvent extends ReasonedCancellableEvent {

    private final Room room;

}
