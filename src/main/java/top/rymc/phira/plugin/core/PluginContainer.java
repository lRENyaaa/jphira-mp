package top.rymc.phira.plugin.core;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.plugin.Plugin;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PluginContainer {
    @Getter
    private final String id;
    @Getter
    @Setter
    private Object instance;
    @Getter
    private final ClassLoader classLoader;
    @Getter
    private final Plugin meta;
    @Getter
    private final Path dataFolder;
    @Getter
    private final Logger logger;
    private final Set<Object> listeners = ConcurrentHashMap.newKeySet();
    @Getter
    private volatile boolean enabled = false;

    public PluginContainer(String id, Object instance, ClassLoader loader, Plugin meta, Path dataFolder) {
        this.id = id;
        this.instance = instance;
        this.classLoader = loader;
        this.meta = meta;
        this.dataFolder = dataFolder;
        this.logger = LogManager.getLogger(id);
    }

    void setEnabled(boolean enabled) { this.enabled = enabled; }

    void addListener(Object listener) { listeners.add(listener); }
    void removeListener(Object listener) { listeners.remove(listener); }
    Set<Object> getListeners() { return Collections.unmodifiableSet(listeners); }

    void clearListeners() {
        listeners.clear();
    }
}
