package top.rymc.phira.main.event.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.record.PhiraRecord;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.plugin.event.Event;

import java.util.Map;

@RequiredArgsConstructor
@Getter
public class GameEndEvent extends Event {

    private final Room room;
    private final ChartInfo chart;
    private final Map<Player, GameRecord> gameRecords;
    private final Map<Player, PhiraRecord> playerRecords;

}
