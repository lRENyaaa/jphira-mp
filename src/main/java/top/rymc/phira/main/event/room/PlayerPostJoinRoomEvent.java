package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class PlayerPostJoinRoomEvent extends ReasonedCancellableEvent {

    private final LocalPlayer player;
    private final LocalRoom room;
    private final boolean isMonitor;
}
