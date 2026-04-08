package top.rymc.phira.main.event.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.plugin.event.CancellableEvent;

@Getter
@RequiredArgsConstructor
public class PlayerSessionSuspendEvent extends CancellableEvent {
    private final LocalPlayer player;
    private final Room room;
}
