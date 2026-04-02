package top.rymc.phira.main.network.handler;

import lombok.Getter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.room.PlayerPostJoinRoomEvent;
import top.rymc.phira.main.event.room.PlayerPreJoinRoomEvent;
import top.rymc.phira.main.event.room.PlayerJoinRoomSuccessEvent;
import top.rymc.phira.main.event.room.RoomPreCreateEvent;
import top.rymc.phira.main.event.room.RoomPostCreateEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.player.holder.PlayerHolder;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.main.game.i18n.I18nService;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.handler.server.SimpleServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundCreateRoomPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundJoinRoomPacket;

public class PlayHandler extends SimpleServerBoundPacketHandler implements PlayerHolder {

    @Getter
    private final LocalPlayer player;

    protected void sendPacket(ClientBoundPacket packet) {
        player.getConnection().send(packet);
    }

    public static PlayHandler create(LocalPlayer player) {
        return new PlayHandler(player);
    }

    private PlayHandler(LocalPlayer player) {
        this.player = player;
    }

    @Override
    public void handle(ServerBoundCreateRoomPacket packet) {
        try {
            RoomPreCreateEvent createEvent = new RoomPreCreateEvent(player, packet.getRoomId(), new Room.RoomSetting());
            Server.postEvent(createEvent);
            String cancelReason = createEvent.getCancelReason();
            if (cancelReason != null) {
                player.getConnection().send(ClientBoundCreateRoomPacket.failed(cancelReason));
                return;
            }

            Room room = RoomManager.createRoom(packet.getRoomId(), player, createEvent.getSetting());

            RoomPostCreateEvent createdEvent = new RoomPostCreateEvent(room, player);
            Server.postEvent(createdEvent);

            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            player.getConnection().send(ClientBoundCreateRoomPacket.success());

            room.getProtocolHack().forceSyncInfo(player, false);

        } catch (GameOperationException e) {
            player.getConnection().send(ClientBoundCreateRoomPacket.failed(I18nService.INSTANCE.getMessage(player, e.getMessageKey())));
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

            PlayerJoinRoomSuccessEvent successEvent = new PlayerJoinRoomSuccessEvent(player, room, packet.isMonitor());
            Server.postEvent(successEvent);

            room.getProtocolHack().fixClientRoomState(player);
            room.getProtocolHack().forceSyncHost(player, false);

        } catch (GameOperationException e) {
            connection.send(ClientBoundJoinRoomPacket.failed(I18nService.INSTANCE.getMessage(player, e.getMessageKey())));
        } catch (Exception e) {
            connection.send(ClientBoundJoinRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        player.kick();
    }
}
