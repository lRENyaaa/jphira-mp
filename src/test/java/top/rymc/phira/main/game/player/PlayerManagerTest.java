package top.rymc.phira.main.game.player;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.exception.session.PlayerTypeMismatchException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.network.ConnectionReference;
import top.rymc.phira.main.network.PlayerConnection;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerManagerTest {

    @Mock
    private UserInfo userInfo;

    @Mock
    private ConnectionReference connectionRef;

    @Mock
    private PlayerConnection connection;

    @Mock
    private Consumer<LocalPlayer> resumer;

    @BeforeEach
    void setUp() throws Exception {
        clearPlayersMap();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearPlayersMap();
    }

    private void clearPlayersMap() throws Exception {
        Field playersField = PlayerManager.class.getDeclaredField("PLAYERS");
        playersField.setAccessible(true);
        Map<?, ?> playersMap = (Map<?, ?>) playersField.get(null);
        playersMap.clear();
    }

    @Test
    @DisplayName("resolvePlayer creates new player when userId not exists")
    void resolvePlayerCreatesNewPlayerWhenUserIdNotExists() {
        int userId = 1001;
        Function<Runnable, LocalPlayer> constructor = remover -> new LocalPlayer(userInfo, connectionRef);

        PlayerManager.ResolveResult<LocalPlayer> result = PlayerManager.resolvePlayer(
                userId, LocalPlayer.class, constructor, resumer
        );

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(PlayerManager.ResolveResult.Type.Create);
        assertThat(result.player()).isNotNull();
        assertThat(result.player().getUserInfo()).isEqualTo(userInfo);
    }

    @Test
    @DisplayName("resolvePlayer resumes existing player when userId exists")
    void resolvePlayerResumesExistingPlayerWhenUserIdExists() {
        int userId = 1002;
        LocalPlayer existingPlayer = new LocalPlayer(userInfo, connectionRef);
        Function<Runnable, LocalPlayer> constructor = remover -> existingPlayer;

        PlayerManager.ResolveResult<LocalPlayer> firstResult = PlayerManager.resolvePlayer(
                userId, LocalPlayer.class, constructor, resumer
        );
        assertThat(firstResult.type()).isEqualTo(PlayerManager.ResolveResult.Type.Create);

        PlayerManager.ResolveResult<LocalPlayer> secondResult = PlayerManager.resolvePlayer(
                userId, LocalPlayer.class, constructor, resumer
        );

        assertThat(secondResult).isNotNull();
        assertThat(secondResult.type()).isEqualTo(PlayerManager.ResolveResult.Type.Resume);
        assertThat(secondResult.player()).isEqualTo(firstResult.player());
        verify(resumer).accept(firstResult.player());
    }

    @Test
    @DisplayName("resolvePlayer throws PlayerTypeMismatchException when type mismatches")
    void resolvePlayerThrowsPlayerTypeMismatchExceptionWhenTypeMismatches() {
        int userId = 1003;
        LocalPlayer existingPlayer = new LocalPlayer(userInfo, connectionRef);
        Function<Runnable, LocalPlayer> constructor = remover -> existingPlayer;

        PlayerManager.resolvePlayer(userId, LocalPlayer.class, constructor, resumer);

        assertThatThrownBy(() -> {
            PlayerManager.resolvePlayer(
                    userId, AnotherPlayerType.class, id -> mock(AnotherPlayerType.class), p -> {}
            );
        }).isInstanceOf(PlayerTypeMismatchException.class)
                .satisfies(ex -> {
                    PlayerTypeMismatchException exception = (PlayerTypeMismatchException) ex;
                    assertThat(exception.getPlayer()).isEqualTo(existingPlayer);
                });
    }

    @Test
    @DisplayName("getPlayer by PlayerConnection returns correct LocalPlayer")
    void getPlayerByPlayerConnectionReturnsCorrectLocalPlayer() {
        int userId = 1004;
        LocalPlayer player = new LocalPlayer(userInfo, connectionRef);
        Function<Runnable, LocalPlayer> constructor = remover -> player;

        PlayerManager.resolvePlayer(userId, LocalPlayer.class, constructor, resumer);

        when(connectionRef.get()).thenReturn(connection);

        Optional<LocalPlayer> result = PlayerManager.getPlayer(connection);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(player);
    }

    @Test
    @DisplayName("getPlayer by PlayerConnection returns empty when connection not found")
    void getPlayerByPlayerConnectionReturnsEmptyWhenConnectionNotFound() {
        PlayerConnection otherConnection = mock(PlayerConnection.class);

        Optional<LocalPlayer> result = PlayerManager.getPlayer(otherConnection);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPlayer by id returns player if exists")
    void getPlayerByIdReturnsPlayerIfExists() {
        int userId = 1005;
        LocalPlayer player = new LocalPlayer(userInfo, connectionRef);
        Function<Runnable, LocalPlayer> constructor = remover -> player;

        PlayerManager.resolvePlayer(userId, LocalPlayer.class, constructor, resumer);

        Optional<Player> result = PlayerManager.getPlayer(userId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(player);
    }

    @Test
    @DisplayName("getPlayer by id returns empty if not exists")
    void getPlayerByIdReturnsEmptyIfNotExists() {
        int nonExistentUserId = 9999;

        Optional<Player> result = PlayerManager.getPlayer(nonExistentUserId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isOnline returns true for online player")
    void isOnlineReturnsTrueForOnlinePlayer() {
        int userId = 1006;
        LocalPlayer player = new LocalPlayer(userInfo, connectionRef);
        Function<Runnable, LocalPlayer> constructor = remover -> player;

        PlayerManager.resolvePlayer(userId, LocalPlayer.class, constructor, resumer);

        when(connectionRef.get()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        boolean result = PlayerManager.isOnline(userId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isOnline returns false for offline player")
    void isOnlineReturnsFalseForOfflinePlayer() {
        int userId = 1007;
        LocalPlayer player = new LocalPlayer(userInfo, connectionRef);
        Function<Runnable, LocalPlayer> constructor = remover -> player;

        PlayerManager.resolvePlayer(userId, LocalPlayer.class, constructor, resumer);

        when(connectionRef.get()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(true);

        boolean result = PlayerManager.isOnline(userId);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isOnline returns false for non-existing player")
    void isOnlineReturnsFalseForNonExistingPlayer() {
        int nonExistentUserId = 9998;

        boolean result = PlayerManager.isOnline(nonExistentUserId);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getOnlinePlayers filters only online players")
    void getOnlinePlayersFiltersOnlyOnlinePlayers() {
        int onlineUserId = 1008;
        int offlineUserId = 1009;

        UserInfo onlineUserInfo = mock(UserInfo.class);
        UserInfo offlineUserInfo = mock(UserInfo.class);
        ConnectionReference onlineConnectionRef = mock(ConnectionReference.class);
        ConnectionReference offlineConnectionRef = mock(ConnectionReference.class);
        PlayerConnection onlineConnection = mock(PlayerConnection.class);
        PlayerConnection offlineConnection = mock(PlayerConnection.class);

        LocalPlayer onlinePlayer = new LocalPlayer(onlineUserInfo, onlineConnectionRef);
        LocalPlayer offlinePlayer = new LocalPlayer(offlineUserInfo, offlineConnectionRef);

        Function<Runnable, LocalPlayer> onlineConstructor = remover -> onlinePlayer;
        Function<Runnable, LocalPlayer> offlineConstructor = remover -> offlinePlayer;

        PlayerManager.resolvePlayer(onlineUserId, LocalPlayer.class, onlineConstructor, resumer);
        PlayerManager.resolvePlayer(offlineUserId, LocalPlayer.class, offlineConstructor, resumer);

        when(onlineConnectionRef.get()).thenReturn(onlineConnection);
        when(offlineConnectionRef.get()).thenReturn(offlineConnection);
        when(onlineConnection.isClosed()).thenReturn(false);
        when(offlineConnection.isClosed()).thenReturn(true);

        List<Player> result = PlayerManager.getOnlinePlayers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(onlinePlayer);
    }

    @Test
    @DisplayName("getOnlinePlayers returns empty list when no players online")
    void getOnlinePlayersReturnsEmptyListWhenNoPlayersOnline() {
        List<Player> result = PlayerManager.getOnlinePlayers();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllPlayers returns all registered players")
    void getAllPlayersReturnsAllRegisteredPlayers() {
        int userId1 = 1010;
        int userId2 = 1011;

        UserInfo userInfo1 = mock(UserInfo.class);
        UserInfo userInfo2 = mock(UserInfo.class);
        ConnectionReference connectionRef1 = mock(ConnectionReference.class);
        ConnectionReference connectionRef2 = mock(ConnectionReference.class);

        LocalPlayer player1 = new LocalPlayer(userInfo1, connectionRef1);
        LocalPlayer player2 = new LocalPlayer(userInfo2, connectionRef2);

        Function<Runnable, LocalPlayer> constructor1 = remover -> player1;
        Function<Runnable, LocalPlayer> constructor2 = remover -> player2;

        PlayerManager.resolvePlayer(userId1, LocalPlayer.class, constructor1, resumer);
        PlayerManager.resolvePlayer(userId2, LocalPlayer.class, constructor2, resumer);

        List<Player> result = PlayerManager.getAllPlayers();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(player1, player2);
    }

    @Test
    @DisplayName("getAllPlayers returns empty list when no players registered")
    void getAllPlayersReturnsEmptyListWhenNoPlayersRegistered() {
        List<Player> result = PlayerManager.getAllPlayers();

        assertThat(result).isEmpty();
    }

    private static class AnotherPlayerType implements Player {
        @Override
        public Optional<top.rymc.phira.main.game.room.Room> getRoom() {
            return Optional.empty();
        }

        @Override
        public void kick() {
        }

        @Override
        public UserInfo getUserInfo() {
            return null;
        }

        @Override
        public Optional<top.rymc.phira.main.game.player.operations.PlayerOperations> operations() {
            return Optional.empty();
        }

        @Override
        public boolean isOnline() {
            return false;
        }

        @Override
        public top.rymc.phira.protocol.data.UserProfile toProtocol() {
            return null;
        }
    }
}
