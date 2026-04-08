package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class RoomPostSelectChartEvent extends Event {

    private final LocalRoom room;
    private final Player selector;
    private final ChartInfo chart;

}
