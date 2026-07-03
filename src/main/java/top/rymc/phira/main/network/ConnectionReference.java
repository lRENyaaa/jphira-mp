package top.rymc.phira.main.network;

import java.util.concurrent.atomic.AtomicReference;

public class ConnectionReference {

    private final AtomicReference<PlayerConnection> connectionReference = new AtomicReference<>();

    public ConnectionReference(PlayerConnection connection) {
        connectionReference.set(connection);
    }

    public PlayerConnection get() {
        return connectionReference.get();
    }

    public boolean swap(PlayerConnection expectedOld, PlayerConnection newConnection) {
        return connectionReference.compareAndSet(expectedOld, newConnection);
    }
}
