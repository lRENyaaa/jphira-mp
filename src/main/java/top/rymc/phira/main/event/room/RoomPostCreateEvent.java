package top.rymc.phira.main.event.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class RoomPostCreateEvent extends Event {

    private final LocalRoom room;
    private final LocalPlayer creator;

}
