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

        // In fact, duplicate should always be false, the logic here was written as a precaution.
        // TODO: warn in logger
        PlayerConnection oldConn = connectionReference.get();
        boolean duplicate = oldConn != null;

        if (duplicate) {
            onDuplicate.accept(oldConn);
            oldConn.markDuplicateLogin();
        }

        connectionReference.set(newConnection);

    }

}
