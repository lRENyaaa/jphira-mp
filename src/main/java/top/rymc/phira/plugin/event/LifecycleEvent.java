package top.rymc.phira.plugin.event;

import lombok.Getter;
import top.rymc.phira.plugin.core.PluginContainer;

@Getter
public class LifecycleEvent extends Event {

    private final PluginContainer container;
    private final State state;

    public enum State {
        ENABLE,
        DISABLE
    }

    public LifecycleEvent(PluginContainer container, State state) {
        this.container = container;
        this.state = state;
    }

    public boolean isEnable() {
        return state == State.ENABLE;
    }

}