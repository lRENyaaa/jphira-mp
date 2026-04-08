package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class RoomPreSelectChartEvent extends ReasonedCancellableEvent {

    private final LocalRoom room;
    private final Player selector;
    private final int chartId;

    @Setter
    private ChartInfo chartInfo;

}
