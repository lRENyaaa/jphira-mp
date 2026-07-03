package top.rymc.phira.main.network.handler;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.event.player.PlayerPostLoginEvent;
import top.rymc.phira.main.event.player.PlayerPreAuthenticateEvent;
import top.rymc.phira.main.event.player.PlayerPreLoginEvent;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.player.PlayerManager;
import top.rymc.phira.main.game.i18n.I18nService;
import top.rymc.phira.main.game.session.LocalSessionManager;
import top.rymc.phira.main.game.exception.session.ResumeFailedException;
import top.rymc.phira.main.game.exception.session.SuspendFailedException;
import top.rymc.phira.main.game.room.ProtocolHackService;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

            PlayerPreAuthenticateEvent preAuthEvent = new PlayerPreAuthenticateEvent(connection, token);
            Server.postEvent(preAuthEvent);

            String preAuthCancelReason = preAuthEvent.getCancelReason();
            if (preAuthCancelReason != null) {
                connection.send(ClientBoundAuthenticatePacket.failed(preAuthCancelReason));
                connection.close();
                return;
            }
            UserInfo eventUserInfo = preAuthEvent.getUserInfo();
            UserInfo userInfo = eventUserInfo != null ? eventUserInfo : PhiraFetcher.GET_USER_INFO.apply(token);

            PlayerPreLoginEvent preLoginEvent = new PlayerPreLoginEvent(userInfo);
            Server.postEvent(preLoginEvent);
            String preLoginCancelReason = preLoginEvent.getCancelReason();
            if (preLoginCancelReason != null) {
                connection.send(ClientBoundAuthenticatePacket.failed(preLoginCancelReason));
                connection.close();
                return;
            }

            PlayerManager.ResolveResult<LocalPlayer> result = PlayerManager.resolvePlayer(
                    userInfo.getId(),
                    LocalPlayer.class,
                    () -> new LocalPlayer(userInfo, new ConnectionReference(connection)),
                    (player, remover) -> {
                        if (!LocalSessionManager.hasSuspendedSession(player.getId())) {
                            if (player.isOnline()) {
                                throw new ResumeFailedException();
                            }

                            Server.getLogger().error(
                                    "Player {} has inactive connection without suspended session, oldConnection {}",
                                    player.getId(), player.getConnection()
                            );
                            boolean left = true;
                            if (player.getRoom().filter(room -> room.containsPlayer(player)).isPresent()) {
                                left = player.getRoom().map(room -> room.leave(player)).orElse(false);
                            }
                            boolean removed = left && remover.getAsBoolean();
                            if (!left || !removed) {
                                Server.getLogger().error(
                                        "Failed to recover dead player {}, leave {}, remove {}",
                                        player.getId(), left, removed
                                );
                                throw new ResumeFailedException();
                            }
                            return;
                        }

                        LocalSessionManager.resume(player, connection);
                    },
                    (remover, player) -> connection.onClose((ctx) -> {
                        PlayerConnection.DisconnectReason reason = connection.getDisconnectReason();
                        if (reason == PlayerConnection.DisconnectReason.DUPLICATE && player.getConnection() != connection) {
                            return;
                        }

                        if (reason == PlayerConnection.DisconnectReason.KICK) {
                            return;
                        }

                        if (reason == PlayerConnection.DisconnectReason.DUPLICATE) {
                            Server.getLogger().error("Current player connection closed as duplicate, player {}", player.getId());
                            player.getRoom().ifPresent(room -> room.leave(player));
                            remover.getAsBoolean();
                            return;
                        }

                        if (player.getConnection() != connection) {
                            return;
                        }

                        try {
                            LocalSessionManager.suspend(player, connection, remover);
                        } catch (SuspendFailedException e) {
                            player.getRoom().ifPresent(room -> room.leave(player));
                            remover.getAsBoolean();
                        }
                    })
            );

            LocalPlayer player = result.player();
            RoomInfo roomInfo = player.getRoom()
                    .map(room -> ProtocolHackService.buildRoomInfo(room, player))
                    .orElseGet(() -> {
                        Server.getLogger().debug("Authenticated player {} has no room, roomInfo=null", player.getId());
                        return null;
                    });

            if (result.type() == PlayerManager.ResolveResult.Type.Create) {
                connection.setPacketHandler(PlayHandler.create(result.player()));
            }

            connection.send(ClientBoundAuthenticatePacket.success(new FullUserProfile(userInfo.getId(), userInfo.getName(), false), roomInfo));

            Server.getLogger().info("{} has logged in as [{}] {}", connection.getRemoteAddressAsString(), userInfo.getId(), userInfo.getName());

            PlayerPostLoginEvent postLoginEvent = new PlayerPostLoginEvent(result);
            Server.postEvent(postLoginEvent);
        } catch (GameOperationException e) {
            connection.send(ClientBoundAuthenticatePacket.failed(I18nService.INSTANCE.getMessage(e.getMessageKey(), e.getArgs())));
            connection.close();
        } catch (ResumeFailedException e) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            connection.send(ClientBoundAuthenticatePacket.failed(I18nService.INSTANCE.getMessage("error.player_already_online", time)));
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
