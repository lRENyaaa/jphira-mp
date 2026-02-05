package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.game.RoomManager;
import top.rymc.phira.main.redis.RedisHolder;
import top.rymc.phira.main.redis.RoomStateCode;
import top.rymc.phira.protocol.handler.SimplePacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundCreateRoomPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundJoinRoomPacket;

public class PlayHandler extends SimplePacketHandler {
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
            if (RedisHolder.isAvailable()) {
                var r = RedisHolder.get();
                r.setRoomInfo(room.getRoomId(), player.getId(), RoomStateCode.SELECT_CHART, 0,
                        room.getSetting().isLocked(), room.getSetting().isCycle());
                r.addRoomPlayer(room.getRoomId(), player.getId());
                r.setPlayerSession(player.getId(), room.getRoomId(), player.getName(), false);
                r.publishRoomCreate(room.getRoomId(), player.getId(), player.getName());
            }
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            player.getConnection().send(new ClientBoundCreateRoomPacket.Success());

            room.getProtocolHack().forceSyncInfo(player);

        } catch (Exception e) {
            player.getConnection().send(new ClientBoundCreateRoomPacket.Failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {
        try {
            Room room = RoomManager.findRoom(packet.getRoomId());
            if (room == null) {
                throw new IllegalStateException("房间不存在");
            }
            if (RedisHolder.isAvailable()) {
                boolean added = RedisHolder.get().tryAddRoomPlayer(
                        packet.getRoomId(), player.getId(), room.getSetting().getMaxPlayer());
                if (!added) {
                    throw new IllegalStateException("房间已满");
                }
            }

            room.join(player, packet.isMonitor());
            if (RedisHolder.isAvailable()) {
                var r = RedisHolder.get();
                r.updatePlayerSessionRoom(player.getId(), room.getRoomId());
                r.updatePlayerSessionMonitor(player.getId(), packet.isMonitor());
                r.publishPlayerJoin(room.getRoomId(), player.getId(), player.getName(), packet.isMonitor());
            }
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            player.getConnection().send(room.getProtocolHack().buildJoinSuccessPacket());

            room.getProtocolHack().fixClientRoomState(player);

        } catch (Exception e) {
            player.getConnection().send(new ClientBoundJoinRoomPacket.Failed(e.getMessage()));
        }
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        // 在 Play 状态下收到无法处理的包（如游戏操作），踢掉
        player.kick();
    }
}