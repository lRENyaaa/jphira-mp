package top.rymc.phira.main.network.handler;

import lombok.Getter;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.*;

@Getter
public class RoomHandler extends ServerBoundPacketHandler {
    private final Player player;
    private final Room room;
    private final ServerBoundPacketHandler fallback;

    public RoomHandler(Player player, Room room, ServerBoundPacketHandler fallback) {
        this.player = player;
        this.room = room;
        this.fallback = fallback;
    }

    @Override
    public void handle(ServerBoundChatPacket packet) {
        try {
            room.getOperation().chat(player, packet.getMessage());
            player.getConnection().send(ClientBoundLockRoomPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundLockRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundLeaveRoomPacket p) {
        try {
            room.leave(player);
            player.getConnection().setPacketHandler(fallback);
            player.getConnection().send(ClientBoundLeaveRoomPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundLeaveRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundLockRoomPacket p) {
        try {
            room.getOperation().lockRoom(player);
            player.getConnection().send(ClientBoundLockRoomPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundLockRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundCycleRoomPacket p) {
        try {
            room.getOperation().cycleRoom(player);
            player.getConnection().send(ClientBoundCycleRoomPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundCycleRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundSelectChartPacket packet) {
        try {
            room.getOperation().selectChart(player, packet.getId());
            player.getConnection().send(ClientBoundSelectChartPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundSelectChartPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundReadyPacket p) {
        try {
            room.getOperation().ready(player);
            player.getConnection().send(ClientBoundReadyPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundReadyPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundCancelReadyPacket p) {
        try {
            room.getOperation().cancelReady(player);
            player.getConnection().send(ClientBoundCancelReadyPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundCancelReadyPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundRequestStartPacket p) {
        try {
            room.getOperation().requireStart(player);
            player.getConnection().send(ClientBoundRequestStartPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundRequestStartPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundPlayedPacket packet) {
        try {
            room.getOperation().played(player, packet.getId());
            player.getConnection().send(ClientBoundPlayedPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundPlayedPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundAbortPacket p) {
        try {
            room.getOperation().abort(player);
            player.getConnection().send(ClientBoundAbortPacket.success());
        } catch (Exception e) {
            player.getConnection().send(ClientBoundAbortPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundPingPacket serverBoundPingPacket) {
        player.getConnection().send(ClientBoundPongPacket.INSTANCE);
    }

    @Override public void handle(ServerBoundAuthenticatePacket p) { kick(); }
    @Override public void handle(ServerBoundTouchesPacket p) {
        room.getOperation().touchSend(player, p.getFrames());
    }
    @Override public void handle(ServerBoundJudgesPacket p) {
        room.getOperation().judgeSend(player, p.getJudges());
    }
    @Override public void handle(ServerBoundCreateRoomPacket p) { kick(); }
    @Override public void handle(ServerBoundJoinRoomPacket p) { kick(); }

    private void kick() {
        player.kick();
    }
}