package top.rymc.phira.plugin.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;

public class PluginClassLoader extends URLClassLoader {
    private static final Set<String> SHARED_PACKAGES = Set.of(
            "top.rymc.phira.",
            "meteordevelopment.orbit.",
            "org.apache.logging.log4j."
    );

    public PluginClassLoader(Path jar, ClassLoader parent) throws Exception {
        super(new URL[]{jar.toUri().toURL()}, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String pkg : SHARED_PACKAGES) {
            if (name.startsWith(pkg)) {
                return getParent().loadClass(name);
            }
        }

        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
            return super.loadClass(name, resolve);
        }
    }
}