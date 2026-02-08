package top.rymc.phira.plugin.event;

import meteordevelopment.orbit.ICancellable;


public abstract class CancellableEvent extends Event implements ICancellable {
    private boolean cancelled = false;

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }
}