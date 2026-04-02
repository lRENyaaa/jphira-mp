package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.plugin.event.Event;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public class RoomDestroyEvent extends Event {

    private final Room room;
    private final Set<LocalPlayer> remainingPlayers;
    private final Set<LocalPlayer> remainingMonitors;

}
