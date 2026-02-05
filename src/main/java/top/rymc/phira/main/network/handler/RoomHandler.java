package top.rymc.phira.main.network.handler;

import lombok.Getter;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.protocol.handler.PacketHandler;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.*;

@Getter
public class RoomHandler extends PacketHandler {
    private final Player player;
    private final Room room;
    private final PacketHandler fallback; // 离开房间后回到 PlayHandler

    public RoomHandler(Player player, Room room, PacketHandler fallback) {
        this.player = player;
        this.room = room;
        this.fallback = fallback;
    }

    // 所有操作委托给 Room，Room 再委托给 RoomGameState
    @Override
    public void handle(ServerBoundChatPacket p) {
        room.broadcast(new top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket(
                new top.rymc.phira.protocol.data.message.ChatMessage(player.getId(), p.getMessage())
        ));
    }

    @Override
    public void handle(ServerBoundLeaveRoomPacket p) {
        room.leave(player);
        player.getConnection().setPacketHandler(fallback);
        player.getConnection().send(new ClientBoundLeaveRoomPacket.Success());
    }

    @Override
    public void handle(ServerBoundLockRoomPacket p) {
        try {
            room.getOperation().lockRoom(player);
        } catch (Exception e) {
            player.getConnection().send(new ClientBoundLockRoomPacket.Failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundCycleRoomPacket p) {
        try {
            room.getOperation().cycleRoom(player);
        } catch (Exception e) {
            player.getConnection().send(new ClientBoundCycleRoomPacket.Failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundSelectChartPacket packet) {
        try {
            room.getOperation().selectChart(player, packet.getId());
        } catch (Exception e) {
            player.getConnection().send(new ClientBoundSelectChartPacket.Failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundReadyPacket p) {
        player.getConnection().send(new ClientBoundReadyPacket.Failed("当前暂未实现"));
    }

    @Override
    public void handle(ServerBoundCancelReadyPacket p) {
        player.getConnection().send(new ClientBoundCancelReadyPacket.Failed("当前暂未实现"));
    }

    @Override
    public void handle(ServerBoundRequestStartPacket p) {
        player.getConnection().send(new ClientBoundRequestStartPacket.Failed("当前暂未实现"));
    }

    @Override
    public void handle(ServerBoundPlayedPacket p) {
        player.getConnection().send(new ClientBoundPlayedPacket.Failed("当前暂未实现"));
    }

    @Override
    public void handle(ServerBoundAbortPacket p) {
        player.getConnection().send(new ClientBoundAbortPacket.Failed("当前暂未实现"));
    }

    @Override
    public void handle(ServerBoundPingPacket serverBoundPingPacket) {
        player.getConnection().send(ClientBoundPongPacket.INSTANCE);
    }

    @Override public void handle(ServerBoundAuthenticatePacket p) { kick(); }
    @Override public void handle(ServerBoundTouchesPacket p) { /* 当前暂未实现 */ }
    @Override public void handle(ServerBoundJudgesPacket p) { /* 当前暂未实现 */ }
    @Override public void handle(ServerBoundCreateRoomPacket p) { kick(); }
    @Override public void handle(ServerBoundJoinRoomPacket p) { kick(); }

    private void kick() {
        player.kick();
    }
}