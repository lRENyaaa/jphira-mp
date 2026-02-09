package top.rymc.phira.main.event;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.Event;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

@Getter
public class PlayerSwitchPacketHandlerEvent extends Event {

    private final PlayerConnection connection;
    private final ServerBoundPacketHandler oldHandler;

    @Setter
    private ServerBoundPacketHandler newHandler;

    public PlayerSwitchPacketHandlerEvent(PlayerConnection connection, ServerBoundPacketHandler oldHandler, ServerBoundPacketHandler newHandler) {
        this.connection = connection;
        this.oldHandler = oldHandler;
        this.newHandler = newHandler;
    }
}
