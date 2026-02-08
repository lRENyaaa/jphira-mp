package top.rymc.phira.main.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.protocol.packet.ClientBoundPacket;

@RequiredArgsConstructor
@Getter
public class PacketSendEvent extends CancellableEvent {

    private final ClientBoundPacket packet;
}
