package top.rymc.phira.main.event.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.Event;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

@RequiredArgsConstructor
@Getter
public class PlayerPostResumeEvent extends Event {
    private final PlayerConnection connection;
    private final LocalPlayer player;
    private final LocalRoom room;
    private final ServerBoundPacketHandler handlerClass;

}
