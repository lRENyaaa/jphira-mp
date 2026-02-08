package top.rymc.phira.plugin;

public interface PluginLifecycle {
    default void onEnable() {}
    default void onDisable() {}
}
