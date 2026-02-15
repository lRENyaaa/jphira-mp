package top.rymc.phira.main.event.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class PlayerDisconnectEvent extends Event {

    private final Player player;
    private final DisconnectReason reason;

    public enum DisconnectReason {
        QUIT,
        TIMEOUT,
        ERROR,
        DUPLICATE
    }

}
