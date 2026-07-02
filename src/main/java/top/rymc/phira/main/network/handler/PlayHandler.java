package top.rymc.phira.main.network.handler;

import lombok.Getter;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.point.PlayerPointService;
import top.rymc.phira.main.game.player.holder.PlayerHolder;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.main.game.i18n.I18nService;
import top.rymc.phira.main.game.room.RoomSnapshot;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.handler.server.SimpleServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundCreateRoomPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundJoinRoomPacket;

public class PlayHandler extends SimpleServerBoundPacketHandler implements PlayerHolder {

    private static final String ROOM_ID = "zenith";

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
        player.getConnection().send(ClientBoundCreateRoomPacket.failed("当前不支持此操作"));
    }

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {
        PlayerConnection connection = player.getConnection();

        try {
            if (!ROOM_ID.equals(packet.getRoomId())) {
                throw GameOperationException.roomNotFound();
            }

            Room room = RoomManager.findRoom(ROOM_ID);
            if (room == null) {
                throw GameOperationException.roomNotFound();
            }

            room.join(player, packet.isMonitor());
            RoomHandler roomHandler = new RoomHandler(player, room, this);
            connection.setPacketHandler(roomHandler);

            RoomSnapshot.ProtocolHack hack = room.getView().getProtocolHack();
            connection.send(hack.buildJoinSuccessPacket());

            hack.fixClientRoomState(player, true);
            sendPointSummary();

        } catch (GameOperationException e) {
            connection.send(ClientBoundJoinRoomPacket.failed(I18nService.INSTANCE.getMessage(player, e.getMessageKey())));
        } catch (Exception e) {
            connection.send(ClientBoundJoinRoomPacket.failed(e.getMessage()));
        }
    }

    private void sendPointSummary() {
        PlayerPointService.PointSummary point = PlayerPointService.getSummary(player);
        player.getConnection().sendChat("当前积分：" + point.points() + "，积分排名：#" + point.rank());
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        player.kick();
    }
}
