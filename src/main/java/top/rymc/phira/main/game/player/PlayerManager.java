package top.rymc.phira.main.game.player;

import top.rymc.phira.main.game.exception.session.PlayerTypeMismatchException;
import top.rymc.phira.main.network.PlayerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class PlayerManager {

    public record ResolveResult<T extends Player>(T player, Type type) {
        public enum Type {
            Resume,
            Create
        }
    }

    private static final Map<Integer, Player> PLAYERS = new ConcurrentHashMap<>();

    public static <T extends Player> ResolveResult<T> resolvePlayer(
            int userId,
            Class<T> clazz,
            Function<Runnable, T> constructor,
            Consumer<T> resumer
    ) {
        AtomicReference<ResolveResult<T>> reference = new AtomicReference<>();
        PLAYERS.compute(userId, (id, existing) -> {
            if (existing == null) {
                T player = constructor.apply(() -> PLAYERS.remove(userId));
                reference.set(new ResolveResult<>(player, ResolveResult.Type.Create));
                return player;
            }

            if (existing.getClass() != clazz) {
                // This is to be expected, upstream must provide a graceful fallback
                throw new PlayerTypeMismatchException(existing);
            }

            T castedExisting = clazz.cast(existing);
            resumer.accept(castedExisting);
            reference.set(new ResolveResult<>(castedExisting, ResolveResult.Type.Resume));
            return castedExisting;

        });

        ResolveResult<T> result = reference.get();
        if (result == null) {
            throw new AssertionError();
        }

        return result;
    }

    public static Optional<LocalPlayer> getPlayer(PlayerConnection connection) {
        return PLAYERS.values().stream()
                .<LocalPlayer>mapMulti((p, consumer) -> {
                    if (p instanceof LocalPlayer lp) {
                        consumer.accept(lp);
                    }
                })
                .filter(player -> player.getConnection() == connection)
                .findFirst();
    }

    public static Optional<Player> getPlayer(int playerId) {
        return Optional.ofNullable(PLAYERS.get(playerId));
    }

    public static boolean isOnline(int playerId) {
        Player p = PLAYERS.get(playerId);
        return p != null && p.isOnline();
    }

    public static List<Player> getOnlinePlayers() {
        return PLAYERS.values()
                .stream()
                .filter(Player::isOnline)
                .toList();
    }


    public static List<Player> getAllPlayers() {
        return new ArrayList<>(PLAYERS.values());

    }
}