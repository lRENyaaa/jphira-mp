package top.rymc.phira.main.network.handler;

import lombok.Getter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.room.PlayerPostJoinRoomEvent;
import top.rymc.phira.main.event.room.PlayerPreJoinRoomEvent;
import top.rymc.phira.main.event.room.PlayerJoinRoomSuccessEvent;
import top.rymc.phira.main.event.room.RoomPreCreateEvent;
import top.rymc.phira.main.event.room.RoomPostCreateEvent;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.player.holder.PlayerHolder;
import top.rymc.phira.main.game.room.ProtocolHackService;
import top.rymc.phira.main.game.room.local.LocalRoomBuilder;
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

import java.util.concurrent.TimeUnit;

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
            RoomPreCreateEvent createEvent = new RoomPreCreateEvent(player, packet.getRoomId(), new LocalRoomBuilder().buildSetting());
            Server.postEvent(createEvent);
            String cancelReason = createEvent.getCancelReason();
            if (cancelReason != null) {
                player.getConnection().send(ClientBoundCreateRoomPacket.failed(cancelReason));
                return;
            }

            Room room = new LocalRoomBuilder()
                    .setting(createEvent.getSetting())
                    .build(packet.getRoomId());

            room.join(player, false);

            RoomPostCreateEvent createdEvent = new RoomPostCreateEvent(room, player);
            Server.postEvent(createdEvent);

            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            player.getConnection().send(ClientBoundCreateRoomPacket.success());

            if (!room.isHost(player)) {
                ProtocolHackService.chain(room, player)
                        .delay(ProtocolHackService.CLIENT_STATE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                        .updateHost(false)
                        .submit();
            }

        } catch (GameOperationException e) {
            player.getConnection().send(ClientBoundCreateRoomPacket.failed(I18nService.INSTANCE.getMessage(player, e.getMessageKey(), e.getArgs())));
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

            connection.send(ProtocolHackService.buildJoinSuccessPacket(room));

            PlayerJoinRoomSuccessEvent successEvent = new PlayerJoinRoomSuccessEvent(player, room, packet.isMonitor());
            Server.postEvent(successEvent);

            ProtocolHackService.reconnect(room, player);
            ProtocolHackService.chain(room, player)
                    .delay(ProtocolHackService.CLIENT_STATE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .updateHost(room.isHost(player))
                    .submit();

        } catch (GameOperationException e) {
            connection.send(ClientBoundJoinRoomPacket.failed(I18nService.INSTANCE.getMessage(player, e.getMessageKey(), e.getArgs())));
        } catch (Exception e) {
            connection.send(ClientBoundJoinRoomPacket.failed(e.getMessage()));
        }
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        player.kick();
    }
}
