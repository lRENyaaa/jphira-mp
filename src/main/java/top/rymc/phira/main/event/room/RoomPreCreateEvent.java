package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@Getter
public class RoomPreCreateEvent extends ReasonedCancellableEvent {

    private final LocalPlayer creator;
    private final String roomId;

    @Setter
    private LocalRoom.RoomSetting setting;

    public RoomPreCreateEvent(LocalPlayer creator, String roomId, LocalRoom.RoomSetting setting) {
        this.creator = creator;
        this.roomId = roomId;
        this.setting = setting;
    }

}
