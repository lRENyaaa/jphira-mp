package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.game.RoomManager;
import top.rymc.phira.main.redis.PubSubEvent;
import top.rymc.phira.main.redis.RedisHolder;
import top.rymc.phira.protocol.handler.SimplePacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundCreateRoomPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundJoinRoomPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundPingPacket;

import java.util.Map;

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
    public void handle(ServerBoundPingPacket packet) {
        player.getConnection().send(ClientBoundPongPacket.INSTANCE);
        RedisHolder.get().updatePlayerLastSeen(player.getId());
    }

    @Override
    public void handle(ServerBoundCreateRoomPacket packet) {
        try {
            Room room = RoomManager.createRoom(packet.getRoomId(), player);
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            player.getConnection().send(new ClientBoundCreateRoomPacket.Success());

            RedisHolder.get().setPlayerSession(player.getId(), RedisHolder.get().getServerId(),
                    room.getRoomId(), player.getName(), false);

            room.getProtocolHack().forceSyncInfo(player);

        } catch (Exception e) {
            player.getConnection().send(new ClientBoundCreateRoomPacket.Failed(e.getMessage()));
        }
    }

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {
        try {
            var redis = RedisHolder.get();
            int maxPlayers = 8;
            if (!redis.joinRoomAtomic(packet.getRoomId(), player.getId(), maxPlayers)) {
                throw new IllegalStateException("房间已满");
            }
            Room room = RoomManager.getOrCreateFromRedis(packet.getRoomId(), player);
            if (room == null) {
                redis.removeRoomPlayer(packet.getRoomId(), player.getId());
                throw new IllegalStateException("房间不存在");
            }

            room.join(player, packet.isMonitor());
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            redis.setPlayerSession(player.getId(), redis.getServerId(),
                    packet.getRoomId(), player.getName(), packet.isMonitor());
            redis.publishEvent(PubSubEvent.PLAYER_JOIN, packet.getRoomId(),
                    Map.of("uid", player.getId(), "name", player.getName(), "is_monitor", packet.isMonitor()));

            player.getConnection().send(room.getProtocolHack().buildJoinSuccessPacket());
            room.getProtocolHack().fixClientRoomState(player);

        } catch (Exception e) {
            player.getConnection().send(new ClientBoundJoinRoomPacket.Failed(e.getMessage()));
        }
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        player.kick();
    }

}