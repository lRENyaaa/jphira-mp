package top.rymc.phira.main.network;

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
        PlayerConnection oldConnection = connectionReference.get();
        boolean duplicate = oldConnection != null;

        if (duplicate) {
            onDuplicate.accept(oldConnection);
            oldConnection.markDuplicateLogin();
        }

        connectionReference.set(newConnection);

    }

}
