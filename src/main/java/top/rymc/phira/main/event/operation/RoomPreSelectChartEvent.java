package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class RoomPreSelectChartEvent extends ReasonedCancellableEvent {

    private final Room room;
    private final LocalPlayer selector;
    private final int chartId;

    @Setter
    private ChartInfo chartInfo;

}
