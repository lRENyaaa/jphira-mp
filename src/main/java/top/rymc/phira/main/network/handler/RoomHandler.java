package top.rymc.phira.main.network.handler;

import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.protocol.handler.PacketHandler;
import top.rymc.phira.protocol.packet.serverbound.*;

@RequiredArgsConstructor
public class RoomHandler extends PacketHandler {

    private final Player player;
    private final Room room;
    private final PacketHandler previousHandler;

    @Override
    public void handle(ServerBoundPingPacket serverBoundPingPacket) {

    }

    @Override
    public void handle(ServerBoundAuthenticatePacket serverBoundAuthenticatePacket) {

    }

    @Override
    public void handle(ServerBoundChatPacket serverBoundChatPacket) {

    }

    @Override
    public void handle(ServerBoundTouchesPacket serverBoundTouchesPacket) {

    }

    @Override
    public void handle(ServerBoundJudgesPacket serverBoundJudgesPacket) {

    }

    @Override
    public void handle(ServerBoundCreateRoomPacket serverBoundCreateRoomPacket) {

    }

    @Override
    public void handle(ServerBoundJoinRoomPacket serverBoundJoinRoomPacket) {

    }

    @Override
    public void handle(ServerBoundLeaveRoomPacket serverBoundLeaveRoomPacket) {

    }

    @Override
    public void handle(ServerBoundLockRoomPacket serverBoundLockRoomPacket) {

    }

    @Override
    public void handle(ServerBoundCycleRoomPacket serverBoundCycleRoomPacket) {

    }

    @Override
    public void handle(ServerBoundSelectChartPacket serverBoundSelectChartPacket) {

    }

    @Override
    public void handle(ServerBoundRequestStartPacket serverBoundRequestStartPacket) {

    }

    @Override
    public void handle(ServerBoundReadyPacket serverBoundReadyPacket) {

    }

    @Override
    public void handle(ServerBoundCancelReadyPacket serverBoundCancelReadyPacket) {

    }

    @Override
    public void handle(ServerBoundPlayedPacket serverBoundPlayedPacket) {

    }

    @Override
    public void handle(ServerBoundAbortPacket serverBoundAbortPacket) {

    }
}
