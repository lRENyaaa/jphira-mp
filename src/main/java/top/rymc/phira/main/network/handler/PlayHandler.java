package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.game.RoomManager;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.handler.SimplePacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundCreateRoomPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJoinRoomPacket;
import top.rymc.phira.protocol.packet.serverbound.*;

import java.util.List;

public class PlayHandler extends SimplePacketHandler {

    private final Player player;

    public static PlayHandler create(Player player) {
        if (player.getRoom() != null) {
            // TODO throw exception
        }

        return new PlayHandler(player);
    }
    
    private PlayHandler(Player player) {
        super(player.getConnection().getChannel());
        this.player = player;
    }

    @Override
    public void handle(ServerBoundCreateRoomPacket packet) {
        try {
            String roomId = packet.getRoomId();
            RoomManager.createRoom(roomId, player);
            channel.writeAndFlush(new ClientBoundCreateRoomPacket.Success());
        } catch (Exception e) {
            channel.writeAndFlush(new ClientBoundCreateRoomPacket.Failed(e.getMessage()));
        }

    }

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {
        try {
            String roomId = packet.getRoomId();
            Room room = RoomManager.findRoom(roomId);
            room.handleJoin(player, packet.isMonitor());

        }catch (Exception e) {
            channel.writeAndFlush(new ClientBoundJoinRoomPacket.Failed(e.getMessage()));
        }
    }


    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        player.kick();
    }
}
