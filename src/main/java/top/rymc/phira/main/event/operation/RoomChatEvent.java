package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@Getter
public class RoomChatEvent extends ReasonedCancellableEvent {

    private final LocalPlayer player;
    private final LocalRoom room;

    @Setter
    private String message;

    public RoomChatEvent(LocalPlayer player, LocalRoom room, String message) {
        this.player = player;
        this.room = room;
        this.message = message;
    }

}
