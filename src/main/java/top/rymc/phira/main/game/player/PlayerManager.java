package top.rymc.phira.main.game.player;

import top.rymc.phira.function.throwable.ThrowableConsumer;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.player.PlayerCreateEvent;
import top.rymc.phira.main.game.exception.session.PlayerTypeMismatchException;
import top.rymc.phira.main.game.exception.session.ResumeFailedException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.network.PlayerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
            Supplier<T> constructor,
            ThrowableConsumer<T, ResumeFailedException> resumer,
            BiConsumer<Runnable, T> closeBinder
    ) throws ResumeFailedException {

        AtomicReference<ResolveResult<T>> reference = new AtomicReference<>();
        AtomicReference<ResumeFailedException> exceptionReference = new AtomicReference<>();

        PLAYERS.compute(userId, (id, existing) -> {
            if (existing == null) {
                T player = constructor.get();
                reference.set(new ResolveResult<>(player, ResolveResult.Type.Create));

                PlayerCreateEvent createEvent = new PlayerCreateEvent(player);
                Server.postEvent(createEvent);

                return player;
            }

            if (existing.getClass() != clazz) {
                // This is to be expected, upstream must provide a graceful fallback
                exceptionReference.set(new PlayerTypeMismatchException(existing));
                return existing;
            }

            T castedExisting = clazz.cast(existing);

            try {
                resumer.accept(castedExisting);
            } catch (ResumeFailedException exception) {
                exceptionReference.set(exception);
                return existing;
            }

            reference.set(new ResolveResult<>(castedExisting, ResolveResult.Type.Resume));

            return existing;

        });

        ResumeFailedException exception = exceptionReference.get();
        if (exception != null) {
            throw exception;
        }

        ResolveResult<T> result = reference.get();
        if (result == null) {
            throw new AssertionError();
        }

        closeBinder.accept(() -> PLAYERS.remove(userId), result.player);

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