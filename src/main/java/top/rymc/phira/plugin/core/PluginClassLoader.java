package top.rymc.phira.plugin.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;

public class PluginClassLoader extends URLClassLoader {
    private static final Set<String> HOST_PROVIDED_PACKAGES = Set.of(
            "top.rymc.phira.",
            "meteordevelopment.orbit.",
            "org.apache.logging.log4j.",
            "com.google.gson."
    );

    public PluginClassLoader(Path pluginJar, ClassLoader hostLoader) throws Exception {
        super(new URL[]{pluginJar.toUri().toURL()}, hostLoader);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> cached = findLoadedClass(name);
            if (cached != null) {
                if (resolve) resolveClass(cached);
                return cached;
            }

            Class<?> loaded = isHostProvided(name)
                    ? tryLoadFromHostThenSelf(name)
                    : tryLoadFromSelfThenHost(name);

            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }

    private boolean isHostProvided(String name) {
        for (String pkg : HOST_PROVIDED_PACKAGES) {
            if (name.startsWith(pkg)) return true;
        }
        return false;
    }

    private Class<?> tryLoadFromHostThenSelf(String name) throws ClassNotFoundException {
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException hostFailed) {
            return findClass(name);
        }
    }

    private Class<?> tryLoadFromSelfThenHost(String name) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException selfFailed) {
            return getParent().loadClass(name);
        }
    }
}