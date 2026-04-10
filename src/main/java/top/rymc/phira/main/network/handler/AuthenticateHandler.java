package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.player.PlayerManager;
import top.rymc.phira.main.game.i18n.I18nService;
import top.rymc.phira.main.game.session.LocalSessionManager;
import top.rymc.phira.main.game.exception.session.ResumeFailedException;
import top.rymc.phira.main.game.exception.session.SuspendFailedException;
import top.rymc.phira.main.network.ConnectionReference;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.handler.server.SimpleServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundAuthenticatePacket;
import top.rymc.phira.protocol.packet.serverbound.*;

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
            PlayerManager.ResolveResult<LocalPlayer> result = PlayerManager.resolvePlayer(
                    userInfo.getId(),
                    LocalPlayer.class,
                    (remover) -> {
                        LocalPlayer player = new LocalPlayer(userInfo, new ConnectionReference(connection));
                        connection.onClose((ctx) -> {
                            try {
                                LocalSessionManager.suspend(player, remover);
                            } catch (SuspendFailedException e) {
                                remover.run();
                            }
                        });
                        return player;
                    },
                    (player) -> LocalSessionManager.resume(player, connection)
            );

            RoomInfo roomInfo = result.player().getRoomInfo().orElse(null);

            if (result.type() == PlayerManager.ResolveResult.Type.Create) {
                connection.setPacketHandler(PlayHandler.create(result.player()));
            }

            connection.send(ClientBoundAuthenticatePacket.success(new FullUserProfile(userInfo.getId(), userInfo.getName(), false), roomInfo));

            Server.getLogger().info("{} has logged in as [{}] {}", connection.getRemoteAddressAsString(), userInfo.getId(), userInfo.getName());

        } catch (GameOperationException e) {
            connection.send(ClientBoundAuthenticatePacket.failed(I18nService.INSTANCE.getMessage(e.getMessageKey())));
            connection.close();
        } catch (ResumeFailedException e) {
            connection.send(ClientBoundAuthenticatePacket.failed(I18nService.INSTANCE.getMessage("error.player_already_online")));
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
