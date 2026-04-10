package top.rymc.phira.main.event.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class GameAbortEvent extends Event {

    private final LocalRoom room;
    private final Player abortPlayer;
    private final ChartInfo chart;

}
