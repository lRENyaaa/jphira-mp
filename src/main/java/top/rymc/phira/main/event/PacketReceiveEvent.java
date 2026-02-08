package top.rymc.phira.main.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.protocol.packet.ServerBoundPacket;

@RequiredArgsConstructor
@Getter
public class PacketReceiveEvent extends CancellableEvent {

    private final ServerBoundPacket packet;

}
