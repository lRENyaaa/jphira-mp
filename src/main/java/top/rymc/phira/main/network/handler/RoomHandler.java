package top.rymc.phira.main.network.handler;

import lombok.Getter;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.i18n.I18nService;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.*;
import top.rymc.phira.protocol.packet.serverbound.*;

import java.util.function.Function;
import java.util.function.Supplier;

@Getter
public class RoomHandler extends ServerBoundPacketHandler {
    private final Player player;
    private final Room room;
    private final ServerBoundPacketHandler fallback;

    public RoomHandler(Player player, Room room, ServerBoundPacketHandler fallback) {
        this.player = player;
        this.room = room;
        this.fallback = fallback;
    }

    @Override
    public void handle(ServerBoundChatPacket packet) {
        handleWithException(
            () -> room.getOperation().chat(player, packet.getMessage()),
            ClientBoundLockRoomPacket::success,
            ClientBoundLockRoomPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundLeaveRoomPacket packet) {
        handleWithException(
            () -> {
                room.leave(player);
                player.getConnection().setPacketHandler(fallback);
            },
            ClientBoundLeaveRoomPacket::success,
            ClientBoundLeaveRoomPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundLockRoomPacket packet) {
        handleWithException(
            () -> room.getOperation().lockRoom(player),
            ClientBoundLockRoomPacket::success,
            ClientBoundLockRoomPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundCycleRoomPacket packet) {
        handleWithException(
            () -> room.getOperation().cycleRoom(player),
            ClientBoundCycleRoomPacket::success,
            ClientBoundCycleRoomPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundSelectChartPacket packet) {
        handleWithException(
            () -> room.getOperation().selectChart(player, packet.getId()),
            ClientBoundSelectChartPacket::success,
            ClientBoundSelectChartPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundReadyPacket packet) {
        handleWithException(
            () -> room.getOperation().ready(player),
            ClientBoundReadyPacket::success,
            ClientBoundReadyPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundCancelReadyPacket packet) {
        handleWithException(
            () -> room.getOperation().cancelReady(player),
            ClientBoundCancelReadyPacket::success,
            ClientBoundCancelReadyPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundRequestStartPacket packet) {
        handleWithException(
            () -> room.getOperation().requireStart(player),
            ClientBoundRequestStartPacket::success,
            ClientBoundRequestStartPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundPlayedPacket packet) {
        handleWithException(
            () -> room.getOperation().played(player, packet.getId()),
            ClientBoundPlayedPacket::success,
            ClientBoundPlayedPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundAbortPacket packet) {
        handleWithException(
            () -> room.getOperation().abort(player),
            ClientBoundAbortPacket::success,
            ClientBoundAbortPacket::failed
        );
    }

    @Override
    public void handle(ServerBoundPingPacket serverBoundPingPacket) {
        player.getConnection().send(ClientBoundPongPacket.INSTANCE);
    }

    @Override
    public void handle(ServerBoundAuthenticatePacket packet) {
        kick();
    }

    @Override
    public void handle(ServerBoundTouchesPacket packet) {
        room.getOperation().touchSend(player, packet.getFrames());
    }

    @Override
    public void handle(ServerBoundJudgesPacket packet) {
        room.getOperation().judgeSend(player, packet.getJudges());
    }

    @Override
    public void handle(ServerBoundCreateRoomPacket packet) {
        kick();
    }

    @Override
    public void handle(ServerBoundJoinRoomPacket packet) {
        kick();
    }

    private void kick() {
        player.kick();
    }

    private void handleWithException(
            Runnable action,
            Supplier<ClientBoundPacket> successPacket,
            Function<String, ClientBoundPacket> failedPacket) {
        try {
            action.run();
            player.getConnection().send(successPacket.get());
        } catch (GameOperationException e) {
            player.getConnection().send(failedPacket.apply(I18nService.INSTANCE.getMessage(player, e.getMessageKey())));
        } catch (Exception e) {
            player.getConnection().send(failedPacket.apply(e.getMessage()));
        }
    }
}
