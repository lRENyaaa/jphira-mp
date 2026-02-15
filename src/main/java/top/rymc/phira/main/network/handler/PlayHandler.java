package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.PlayerPostJoinRoomEvent;
import top.rymc.phira.main.event.PlayerPreJoinRoomEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.game.RoomManager;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.handler.server.SimpleServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundCreateRoomPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundJoinRoomPacket;

public class PlayHandler extends SimpleServerBoundPacketHandler {
    private final Player player;

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
        PlayerConnection connection = player.getConnection();

        try {
            PlayerPreJoinRoomEvent preJoinRoomEvent = new PlayerPreJoinRoomEvent(player, packet.getRoomId(), packet.isMonitor());
            Server.postEvent(preJoinRoomEvent);
            String preJoinCancelMessage = preJoinRoomEvent.getCancelReason();
            if (preJoinCancelMessage != null) {
                connection.send(ClientBoundJoinRoomPacket.failed(preJoinCancelMessage));
                return;
            }

            Room room = RoomManager.findRoom(packet.getRoomId());
            if (room == null) {
                throw GameOperationException.roomNotFound();
            }

            PlayerPostJoinRoomEvent postJoinRoomEvent = new PlayerPostJoinRoomEvent(player, room, packet.isMonitor());
            Server.postEvent(postJoinRoomEvent);
            String postJoinCancelMessage = postJoinRoomEvent.getCancelReason();
            if (postJoinCancelMessage != null) {
                connection.send(ClientBoundJoinRoomPacket.failed(postJoinCancelMessage));
                return;
            }

            room.join(player, packet.isMonitor());
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            connection.setPacketHandler(roomHandler);

            connection.send(room.getProtocolHack().buildJoinSuccessPacket());

            room.getProtocolHack().fixClientRoomState(player);
            room.getProtocolHack().forceSyncHost(player);

        } catch (Exception e) {
            connection.send(ClientBoundJoinRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        player.kick();
    }
}