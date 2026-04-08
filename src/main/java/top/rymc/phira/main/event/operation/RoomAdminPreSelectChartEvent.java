package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@Getter
@RequiredArgsConstructor
public class RoomAdminPreSelectChartEvent extends ReasonedCancellableEvent {

    private final LocalRoom room;
    private final int chartId;

    @Setter
    private ChartInfo chartInfo;
}
