package top.rymc.phira.main.event.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class RoomCycleChangeEvent extends Event {

    private final LocalRoom room;
    private final LocalPlayer operator;
    private final boolean cycle;

}
