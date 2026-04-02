package top.rymc.phira.main.event.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.plugin.event.Event;

@RequiredArgsConstructor
@Getter
public class PlayerDisconnectEvent extends Event {

    private final LocalPlayer player;
    private final DisconnectReason reason;

    public enum DisconnectReason {
        QUIT,
        KICK,
        TIMEOUT,
        ERROR,
        DUPLICATE
    }

}
