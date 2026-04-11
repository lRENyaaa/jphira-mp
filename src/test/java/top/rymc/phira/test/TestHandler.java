package top.rymc.phira.test;

import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.holder.SuspendableRoomHolder;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.serverbound.*;

public class TestHandler extends ServerBoundPacketHandler implements SuspendableRoomHolder {
    private final LocalPlayer player;
    private final Room room;

    public TestHandler(LocalPlayer player, Room room) {
        this.player = player;
        this.room = room;
    }

    @Override
    public Room getRoom() {
        return room;
    }

    public LocalPlayer getPlayer() {
        return player;
    }

    @Override
    public void handle(ServerBoundChatPacket packet) {}

    @Override
    public void handle(ServerBoundLeaveRoomPacket packet) {}

    @Override
    public void handle(ServerBoundLockRoomPacket packet) {}

    @Override
    public void handle(ServerBoundCycleRoomPacket packet) {}

    @Override
    public void handle(ServerBoundSelectChartPacket packet) {}

    @Override
    public void handle(ServerBoundReadyPacket packet) {}

    @Override
    public void handle(ServerBoundCancelReadyPacket packet) {}

    @Override
    public void handle(ServerBoundRequestStartPacket packet) {}

    @Override
    public void handle(ServerBoundPlayedPacket packet) {}

    @Override
    public void handle(ServerBoundAbortPacket packet) {}

    @Override
    public void handle(ServerBoundPingPacket packet) {}

    @Override
    public void handle(ServerBoundAuthenticatePacket packet) {}

    @Override
    public void handle(ServerBoundTouchesPacket packet) {}

    @Override
    public void handle(ServerBoundJudgesPacket packet) {}

    @Override
    public void handle(ServerBoundCreateRoomPacket packet) {}

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {}
}
