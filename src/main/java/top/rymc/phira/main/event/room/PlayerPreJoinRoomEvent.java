package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class PlayerPreJoinRoomEvent extends ReasonedCancellableEvent {

    private final Player player;
    private final String roomId;
    private final boolean isMonitor;
}
