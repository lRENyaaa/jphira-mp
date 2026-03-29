package top.rymc.phira.main.event.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.plugin.event.Event;

@Getter
@RequiredArgsConstructor
public class ServerLifecycleEvent extends Event {

    public enum State {
        STARTED,
        STOPPING,
        STOPPED
    }

    private final State state;
}
