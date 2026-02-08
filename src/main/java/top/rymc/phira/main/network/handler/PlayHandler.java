package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.game.RoomManager;
import top.rymc.phira.protocol.handler.server.SimpleServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundCreateRoomPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundJoinRoomPacket;

public class PlayHandler extends SimpleServerBoundPacketHandler {
    private final Player player;

    /**
     * 创建 PlayHandler
     * @return PlayHandler 实例，如果恢复了 RoomSession 则返回 null（因为 Handler 已被替换）
     */
    public static PlayHandler create(Player player) {
        return new PlayHandler(player);
    }

    private PlayHandler(Player player) {
        super(player.getConnection().getChannel());
        this.player = player;
    }

    @Override
    public void handle(ServerBoundCreateRoomPacket packet) {
        try {
            Room room = RoomManager.createRoom(packet.getRoomId(), player);
            // 创建成功后切换到 RoomHandler
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            player.getConnection().send(ClientBoundCreateRoomPacket.success());

            room.getProtocolHack().forceSyncInfo(player);

        } catch (Exception e) {
            player.getConnection().send(ClientBoundCreateRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {
        try {
            Room room = RoomManager.findRoom(packet.getRoomId());
            if (room == null) {
                throw new IllegalStateException("房间不存在");
            }

            room.join(player, packet.isMonitor());
            // 加入成功后切换到 RoomHandler
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            player.getConnection().send(room.getProtocolHack().buildJoinSuccessPacket());

            room.getProtocolHack().fixClientRoomState(player);

        } catch (Exception e) {
            player.getConnection().send(ClientBoundJoinRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        // 在 Play 状态下收到无法处理的包（如游戏操作），踢掉
        player.kick();
    }
}