package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.PlayerManager;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.redis.RedisHolder;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.handler.SimplePacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundAuthenticatePacket;
import top.rymc.phira.protocol.packet.serverbound.*;

import java.util.Optional;

public class AuthenticateHandler extends SimplePacketHandler {

    private final PlayerConnection connection;

    public AuthenticateHandler(PlayerConnection connection) {
        super(connection.getChannel());
        this.connection = connection;
    }

    @Override
    public void handle(ServerBoundPingPacket packet) {
        onUnhandledPacket(packet);
    }

    @Override
    public void handle(ServerBoundAuthenticatePacket packet) {
        try {
            System.out.printf("%s sent his token [%s]%n",connection.getRemoteAddressAsString(), packet.getToken());
            UserInfo userInfo = PhiraFetcher.GET_USER_INFO.apply(packet.getToken());

            Player player = PlayerManager.resumeOrCreate(userInfo, connection);

            Optional<Room> roomOptional = player.getRoom();
            RoomInfo info = null;
            if (roomOptional.isPresent()) {
                Room room = roomOptional.get();
                info = room.asProtocolConvertible(player).toProtocol();
                room.getProtocolHack().forceSyncInfo(player);
            }

            connection.send(new ClientBoundAuthenticatePacket.Success(new FullUserProfile(userInfo.getId(), userInfo.getName(), false), info));

            if (RedisHolder.isAvailable()) {
                String roomId = roomOptional.map(Room::getRoomId).orElse(null);
                boolean isMonitor = roomOptional.map(r -> r.containsMonitor(player)).orElse(false);
                RedisHolder.get().setPlayerSession(userInfo.getId(), roomId, userInfo.getName(), isMonitor);
            }

            System.out.printf("%s has logged in as [%s] %s%n", connection.getRemoteAddressAsString(), userInfo.getId(), userInfo.getName());

        } catch (Exception e) {
            connection.send(new ClientBoundAuthenticatePacket.Failed(e.getMessage()));
            connection.close();
        }
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        connection.close();
    }

}
