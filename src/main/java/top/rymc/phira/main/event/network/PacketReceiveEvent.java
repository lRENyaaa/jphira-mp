package top.rymc.phira.main.event.network;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.protocol.packet.ServerBoundPacket;

@RequiredArgsConstructor
@Getter
public class PacketReceiveEvent extends CancellableEvent {

    private final PlayerConnection connection;
    private final ServerBoundPacket packet;

}
