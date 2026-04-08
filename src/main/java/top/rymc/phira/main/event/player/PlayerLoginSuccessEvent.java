package top.rymc.phira.main.event.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.plugin.event.Event;

import java.util.Optional;

@RequiredArgsConstructor
@Getter
public class PlayerLoginSuccessEvent extends Event {

    private final LocalPlayer player;
    private final boolean resumed;
    private final Optional<LocalRoom> previousRoom;

}
