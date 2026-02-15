package top.rymc.phira.main.event.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.plugin.event.Event;

import java.util.Optional;

@RequiredArgsConstructor
@Getter
public class PlayerLoginSuccessEvent extends Event {

    private final Player player;
    private final boolean resumed;
    private final Optional<Room> previousRoom;

}
