package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@Getter
public class RoomChatEvent extends ReasonedCancellableEvent {

    private final Player player;
    private final Room room;

    @Setter
    private String message;

    public RoomChatEvent(Player player, Room room, String message) {
        this.player = player;
        this.room = room;
        this.message = message;
    }

}
