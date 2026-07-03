package top.rymc.phira.main.game.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.game.room.state.RoomWaitForReady;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.protocol.data.RoomInfo;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public class RoomSnapshot {
    private final String roomId;
    private final RoomGameState state;
    private final boolean live;
    private final boolean locked;
    private final boolean cycle;
    private final Integer host;
    private final Set<Player> players;
    private final Set<Player> monitors;

    public ProtocolConvertible<RoomInfo> asProtocolConvertible(Player viewer) {
        return () -> new RoomInfo(
                roomId,
                state.toProtocol(),
                live, locked, cycle,
                isHost(viewer),
                state instanceof RoomWaitForReady,
                players.stream().map(Player::toProtocol).toList(),
                monitors.stream().map(Player::toProtocol).toList()
        );
    }

    public boolean isHost(Player player) {
        return host != null && player.getId() == host;
    }
}
