package top.rymc.phira.main.event.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.Event;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

@RequiredArgsConstructor
@Getter
public class PlayerPostResumeEvent extends Event {
    private final PlayerConnection connection;
    private final LocalPlayer player;
    private final Room room;
    private final ServerBoundPacketHandler handlerClass;

}
