package top.rymc.phira.main.event.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.Event;

@Getter
@RequiredArgsConstructor
public class PlayerCreateEvent extends Event {
    private final Player player;
}
