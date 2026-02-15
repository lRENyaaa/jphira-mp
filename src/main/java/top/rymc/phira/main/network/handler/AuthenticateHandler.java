package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.event.PlayerPostJoinEvent;
import top.rymc.phira.main.event.PlayerPreJoinEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.PlayerManager;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.i18n.I18nService;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.handler.server.SimpleServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundAuthenticatePacket;
import top.rymc.phira.protocol.packet.serverbound.*;

import java.util.Optional;

public class AuthenticateHandler extends SimpleServerBoundPacketHandler {

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
            Server.getLogger().info("{} sent his token [{}]", connection.getRemoteAddressAsString(), packet.getToken());
            UserInfo userInfo = PhiraFetcher.GET_USER_INFO.apply(packet.getToken());

            PlayerPreJoinEvent preJoinEvent = new PlayerPreJoinEvent(userInfo);
            Server.postEvent(preJoinEvent);
            String preJoinCancelReason = preJoinEvent.getCancelReason();
            if (preJoinCancelReason != null) {
                connection.send(ClientBoundAuthenticatePacket.failed(preJoinCancelReason));
                connection.close();
                return;
            }

            Player player = PlayerManager.resumeOrCreate(userInfo, connection);

            Optional<Room> roomOptional = player.getRoom();
            RoomInfo info = null;
            Room room = null;
            if (roomOptional.isPresent()) {
                room = roomOptional.get();
                info = room.asProtocolConvertible(player).toProtocol();
            }

            PlayerPostJoinEvent postJoinEvent = new PlayerPostJoinEvent(player);
            Server.postEvent(postJoinEvent);
            String postJoinCancelReason = postJoinEvent.getCancelReason();
            if (postJoinCancelReason != null) {
                connection.send(ClientBoundAuthenticatePacket.failed(postJoinCancelReason));
                connection.close();
                return;
            }

            connection.send(ClientBoundAuthenticatePacket.success(new FullUserProfile(userInfo.getId(), userInfo.getName(), false), info));

            Server.getLogger().info("{} has logged in as [{}] {}", connection.getRemoteAddressAsString(), userInfo.getId(), userInfo.getName());

            if (room != null) {
                room.getProtocolHack().forceSyncInfo(player);
            }

        } catch (GameOperationException e) {
            connection.send(ClientBoundAuthenticatePacket.failed(I18nService.INSTANCE.getMessage(e.getMessageKey())));
            connection.close();
        } catch (Exception e) {
            connection.send(ClientBoundAuthenticatePacket.failed(e.getMessage()));
            connection.close();
        }
    }

    @Override
    protected void onUnhandledPacket(ServerBoundPacket packet) {
        connection.close();
    }

}
