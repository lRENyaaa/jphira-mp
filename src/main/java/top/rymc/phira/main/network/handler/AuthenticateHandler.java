package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.event.player.PlayerLoginSuccessEvent;
import top.rymc.phira.main.event.player.PlayerPostLoginEvent;
import top.rymc.phira.main.event.player.PlayerPreAuthenticateEvent;
import top.rymc.phira.main.event.player.PlayerPreLoginEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.player.PlayerManager;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.i18n.I18nService;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.handler.server.SimpleServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundAuthenticatePacket;
import top.rymc.phira.protocol.packet.serverbound.*;

import java.util.Optional;

public class AuthenticateHandler extends SimpleServerBoundPacketHandler {

    private final PlayerConnection connection;

    protected void sendPacket(ClientBoundPacket packet) {
        connection.send(packet);
    }

    public AuthenticateHandler(PlayerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(ServerBoundPingPacket packet) {
        onUnhandledPacket(packet);
    }

    @Override
    public void handle(ServerBoundAuthenticatePacket packet) {
        try {
            String token = packet.getToken();
            Server.getLogger().info("{} sent his token [{}]", connection.getRemoteAddressAsString(), token);

            UserInfo userInfo = PhiraFetcher.GET_USER_INFO.apply(token);
            RoomInfo roomInfo = PlayerManager.resumeOrCreate(userInfo, connection);

            connection.send(ClientBoundAuthenticatePacket.success(new FullUserProfile(userInfo.getId(), userInfo.getName(), false), roomInfo));

            Server.getLogger().info("{} has logged in as [{}] {}", connection.getRemoteAddressAsString(), userInfo.getId(), userInfo.getName());

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
