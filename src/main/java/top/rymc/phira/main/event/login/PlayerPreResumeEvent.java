package top.rymc.phira.main.event.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

@RequiredArgsConstructor
@Getter
public class PlayerPreResumeEvent extends CancellableEvent {
    private final PlayerConnection connection;
    private final Player player;
    private final Room room;
    private final Class<? extends ServerBoundPacketHandler> handlerClass;
}
