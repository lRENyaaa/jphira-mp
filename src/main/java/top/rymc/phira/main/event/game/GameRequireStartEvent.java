package top.rymc.phira.main.event.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class GameRequireStartEvent extends ReasonedCancellableEvent {

    private final LocalRoom room;
    private final Player initiator;
    private final ChartInfo chart;

}
