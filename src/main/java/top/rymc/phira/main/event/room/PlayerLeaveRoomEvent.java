package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class PlayerLeaveRoomEvent extends Event {
    private final Player player;
    private final Room room;
}
