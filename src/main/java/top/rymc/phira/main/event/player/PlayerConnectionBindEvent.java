package top.rymc.phira.main.event.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.Event;

@Getter
@RequiredArgsConstructor
public class PlayerConnectionBindEvent extends Event {
    private final PlayerConnection newConnection;
    private final PlayerConnection oldConnection;
    private final boolean duplicateLogin;
}
