package top.rymc.phira.plugin.core;

import lombok.Getter;
import meteordevelopment.orbit.EventBus;
import top.rymc.phira.plugin.Listener;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.plugin.event.Event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PluginEventBus {
    @Getter
    private final EventBus orbit = new EventBus();

    private final Set<String> registeredPrefixes = ConcurrentHashMap.newKeySet();


    public void subscribe(PluginContainer plugin, Listener listener) {
        Class<?> clazz = listener.getClass();
        String prefix = clazz.getName();

        if (registeredPrefixes.add(prefix)) {
            orbit.registerLambdaFactory(prefix, (lookupInMethod, klass) ->
                    listener.lookup()
            );
        }

        orbit.subscribe(listener);
        plugin.addListener(listener);
    }

    public void unsubscribe(PluginContainer plugin, Listener listener) {
        orbit.unsubscribe(listener);
        plugin.removeListener(listener);
    }

    public void unsubscribeAll(PluginContainer plugin) {
        for (Object listener : plugin.getListeners()) {
            orbit.unsubscribe(listener);
        }
        plugin.clearListeners();
    }

    public <T extends Event> T post(T event) {
        return orbit.post(event);
    }

    public <T extends CancellableEvent> T post(T event) {
        return orbit.post(event);
    }


}