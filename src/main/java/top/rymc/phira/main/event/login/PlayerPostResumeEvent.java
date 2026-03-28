package top.rymc.phira.main.event.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.Event;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

@RequiredArgsConstructor
@Getter
public class PlayerPostResumeEvent extends Event {
    private final PlayerConnection connection;
    private final Player player;
    private final Room room;
    private final Class<? extends ServerBoundPacketHandler> handlerClass;

    @Setter
    private ServerBoundPacketHandler packetHandler;
}
