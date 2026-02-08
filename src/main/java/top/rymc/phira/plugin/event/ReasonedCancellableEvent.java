package top.rymc.phira.plugin.event;

import lombok.Getter;

@Getter
public abstract class ReasonedCancellableEvent extends CancellableEvent {

    private String cancelReason;

    public void cancel(String reason) {
        this.cancelReason = (reason != null && !reason.isEmpty())
                ? reason
                : generateDefaultReason();
        super.cancel();
    }

    @Override
    public void setCancelled(boolean cancelled) {
        if (cancelled && this.cancelReason == null) {
            this.cancelReason = generateDefaultReason();
        }
        super.setCancelled(cancelled);
    }


    protected String generateDefaultReason() {
        return "Cancelled by a plugin";
    }
}
