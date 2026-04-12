package top.rymc.phira.main.network;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.player.PlayerConnectionBindEvent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ConnectionReference {

    private final AtomicReference<PlayerConnection> connectionReference = new AtomicReference<>();


    public ConnectionReference(PlayerConnection connection) {
        connectionReference.set(connection);
    }

    public PlayerConnection get() {
        return connectionReference.get();
    }

    public void resume(PlayerConnection newConnection, Consumer<PlayerConnection> onDuplicate) {

        // In fact, duplicate should always be false, the logic here was written as a precaution.
        PlayerConnection oldConnection = connectionReference.get();
        boolean duplicate = oldConnection != null;

        PlayerConnectionBindEvent bindEvent = new PlayerConnectionBindEvent(newConnection, oldConnection, duplicate);
        Server.postEvent(bindEvent);

        if (duplicate) {
            Server.getLogger().warn("Duplicate connection detected for player, marking old connection as duplicate");
            onDuplicate.accept(oldConnection);
            oldConnection.markDuplicateLogin();
        }

        connectionReference.set(newConnection);

    }

}
