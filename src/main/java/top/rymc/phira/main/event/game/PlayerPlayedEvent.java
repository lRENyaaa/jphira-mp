package top.rymc.phira.main.event.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class PlayerPlayedEvent extends Event {

    private final Player player;
    private final Room room;
    private final int recordId;
    private final int score;
    private final float accuracy;
    private final boolean fullCombo;

}
