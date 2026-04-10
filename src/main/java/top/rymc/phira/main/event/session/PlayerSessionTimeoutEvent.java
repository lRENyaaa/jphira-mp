package top.rymc.phira.main.event.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.plugin.event.Event;

@Getter
@RequiredArgsConstructor
public class PlayerSessionTimeoutEvent extends Event {
    private final LocalPlayer player;
    private final Room room;
}
