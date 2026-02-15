package top.rymc.phira.main.event.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.plugin.event.Event;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public class GamePlayingStartEvent extends Event {

    private final Room room;
    private final ChartInfo chart;
    private final Set<Player> players;
    private final Set<Player> monitors;

}
