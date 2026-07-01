package top.rymc.phira.main.network.handler;

import lombok.Getter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.player.holder.PlayerHolder;
import top.rymc.phira.main.game.room.local.LocalRoom;
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
            Room room = new LocalRoomBuilder().build(packet.getRoomId());

            room.join(player, false);

            RoomHandler roomHandler = new RoomHandler(player, room, this);
            player.getConnection().setPacketHandler(roomHandler);

            player.getConnection().send(ClientBoundCreateRoomPacket.success());

            room.getView().getProtocolHack().forceSyncInfo(player, false);

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
            Room room = RoomManager.findRoom(packet.getRoomId());
            if (room == null) {
                throw GameOperationException.roomNotFound();
            }

            room.join(player, packet.isMonitor());
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            connection.setPacketHandler(roomHandler);

            connection.send(room.getView().getProtocolHack().buildJoinSuccessPacket());

            room.getView().getProtocolHack().fixClientRoomState(player, true);
            room.getView().getProtocolHack().forceSyncHost(player, true);

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
