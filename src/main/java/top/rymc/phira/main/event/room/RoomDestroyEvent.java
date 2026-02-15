package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.plugin.event.Event;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public class RoomDestroyEvent extends Event {

    private final Room room;
    private final Set<Player> remainingPlayers;
    private final Set<Player> remainingMonitors;

}
