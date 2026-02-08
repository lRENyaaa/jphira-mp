package top.rymc.phira.plugin.core;

import com.google.gson.Gson;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.plugin.Listener;
import top.rymc.phira.plugin.Plugin;
import top.rymc.phira.plugin.PluginLifecycle;
import top.rymc.phira.plugin.event.LifecycleEvent;
import top.rymc.phira.plugin.injector.MiniInjector;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class PluginManager {
    private static final Gson GSON = new Gson();

    private final Map<String, PluginContainer> plugins = new ConcurrentHashMap<>();
    private final MiniInjector injector = new MiniInjector();
    @Getter
    private final PluginEventBus eventBus;
    private final Logger serverLogger;
    private final Path pluginsDir;

    public PluginManager(Logger logger, Path pluginsDir) {
        this.serverLogger = logger;
        this.pluginsDir = pluginsDir;
        this.eventBus = new PluginEventBus();

        bindCoreServices();
        ensureDirectoryExists();
    }

    private void bindCoreServices() {
        injector.bind(PluginManager.class, this);
        injector.bind(PluginEventBus.class, eventBus);
        injector.bind(Logger.class, serverLogger);
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(pluginsDir);
        } catch (IOException e) {
            serverLogger.error("Failed to create plugins directory", e);
        }
    }

    public void loadAll() {
        List<Path> jars = listJarFiles();
        if (jars.isEmpty()) return;

        List<PluginCandidate> candidates = loadCandidates(jars);
        List<PluginCandidate> sorted = sortByDependencies(candidates);

        sorted.forEach(this::enablePlugin);
    }

    private List<Path> listJarFiles() {
        try (var stream = Files.list(pluginsDir)) {
            return stream.filter(this::isJarFile).toList();
        } catch (IOException e) {
            serverLogger.error("Failed to scan plugins directory", e);
            return Collections.emptyList();
        }
    }

    private boolean isJarFile(Path path) {
        return path.toString().endsWith(".jar");
    }

    private List<PluginCandidate> loadCandidates(List<Path> jars) {
        return jars.stream()
                .map(this::tryLoadCandidate)
                .filter(Objects::nonNull)
                .toList();
    }

    private PluginCandidate tryLoadCandidate(Path jar) {
        try {
            return loadCandidate(jar);
        } catch (Exception e) {
            serverLogger.error("Failed to load candidate from {}", jar.getFileName(), e);
            return null;
        }
    }

    private PluginCandidate loadCandidate(Path jar) throws Exception {
        PluginDescription desc = readDescription(jar);
        if (desc == null) throw new IllegalStateException("No plugin.json");
        if (desc.main == null) throw new IllegalStateException("Missing 'main' in plugin.json");

        PluginClassLoader loader = new PluginClassLoader(jar, getClass().getClassLoader());
        Class<?> mainClass = loader.loadClass(desc.main);

        Plugin annotation = mainClass.getAnnotation(Plugin.class);
        if (annotation == null) {
            throw new IllegalStateException("Main class missing @Plugin annotation");
        }

        return new PluginCandidate(jar, loader, mainClass, annotation, desc.dependencies);
    }

    private PluginDescription readDescription(Path jar) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            ZipEntry entry = jarFile.getEntry("plugin.json");
            if (entry == null) return null;

            try (InputStream is = jarFile.getInputStream(entry)) {
                return GSON.fromJson(new InputStreamReader(is), PluginDescription.class);
            }
        } catch (IOException e) {
            serverLogger.error("Failed to read plugin.json from {}", jar.getFileName(), e);
            return null;
        }
    }

    private List<PluginCandidate> sortByDependencies(List<PluginCandidate> candidates) {
        Map<String, PluginCandidate> byId = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (PluginCandidate c : candidates) {
            byId.put(c.id(), c);
            inDegree.put(c.id(), c.dependencies().size());
        }

        Queue<PluginCandidate> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(byId.get(e.getKey()));
        }

        List<PluginCandidate> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            PluginCandidate current = queue.poll();
            result.add(current);

            for (PluginCandidate c : candidates) {
                if (!c.dependencies().contains(current.id())) continue;

                int degree = inDegree.get(c.id()) - 1;
                inDegree.put(c.id(), degree);
                if (degree == 0) queue.add(byId.get(c.id()));
            }
        }

        if (result.size() != candidates.size()) {
            throw new IllegalStateException("Circular plugin dependency detected");
        }
        return result;
    }

    private void enablePlugin(PluginCandidate candidate) {
        try {
            tryEnablePlugin(candidate);
        } catch (Exception e) {
            serverLogger.error("Failed to enable plugin {}", candidate.id(), e);
        }
    }

    private void tryEnablePlugin(PluginCandidate candidate) throws Exception {
        checkDependencies(candidate);

        // 1. 准备数据目录
        Path dataFolder = prepareDataFolder(candidate.id());

        // 2. 创建临时容器（instance 为 null）并绑定到注入器
        // 关键：必须在实例化前绑定，供 @Inject PluginContainer 使用
        PluginContainer container = bindContainerAndServices(candidate, dataFolder);

        // 3. 实例化插件（此时可以注入 Container 和 Logger）
        Object instance = injector.getInstance(candidate.mainClass());

        // 4. 更新容器中的实例（创建包含 instance 的新容器）
        container = createContainerWithInstance(container, instance);
        plugins.put(candidate.id(), container);

        // 5. 后续初始化
        subscribeListeners(container, instance);
        fireLifecycleEvent(container, LifecycleEvent.State.ENABLE);
        callLifecycleHook(instance, PluginLifecycle::onEnable);

        serverLogger.info("Enabled plugin {} v{}", candidate.id(), candidate.version());
    }

    private void checkDependencies(PluginCandidate candidate) {
        for (String dep : candidate.dependencies()) {
            if (plugins.containsKey(dep)) continue;
            throw new IllegalStateException("Missing dependency: " + dep);
        }
    }

    private Path prepareDataFolder(String pluginId) {
        Path folder = pluginsDir.resolve(pluginId);
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            serverLogger.warn("Failed to create data folder for {}", pluginId);
        }
        return folder;
    }

    private PluginContainer bindContainerAndServices(PluginCandidate candidate, Path dataFolder) {
        // 创建临时容器（instance 先传 null）
        PluginContainer container = new PluginContainer(
                candidate.id(),
                null,  // 先不传实例
                candidate.loader(),
                candidate.annotation(),
                dataFolder
        );

        // 绑定容器（供插件注入使用）
        injector.bind(PluginContainer.class, container);

        // 绑定插件专属 Logger
        Logger pluginLogger = LogManager.getLogger(candidate.id());
        injector.bind(Logger.class, pluginLogger);

        return container;
    }

    private PluginContainer createContainerWithInstance(PluginContainer old, Object instance) {
        // 创建包含实例的新容器（如果 PluginContainer 是不可变对象）
        return new PluginContainer(
                old.getId(),
                instance,
                old.getClassLoader(),
                old.getMeta(),
                old.getDataFolder()
        );
    }

    private void subscribeListeners(PluginContainer container, Object instance) {
        if (!(instance instanceof Listener listener)) return;
        eventBus.subscribe(container, listener);
    }

    private void fireLifecycleEvent(PluginContainer container, LifecycleEvent.State state) {
        eventBus.post(new LifecycleEvent(container, state));
    }

    private void callLifecycleHook(Object instance, LifecycleHook hook) {
        if (instance instanceof PluginLifecycle lifecycle) {
            hook.accept(lifecycle);
        }
    }

    @FunctionalInterface
    private interface LifecycleHook {
        void accept(PluginLifecycle lifecycle);
    }

    public void disablePlugin(String id) {
        PluginContainer container = plugins.remove(id);
        if (container == null) return;
        disableContainer(container);
    }

    private void disableContainer(PluginContainer container) {
        try {
            fireLifecycleEvent(container, LifecycleEvent.State.DISABLE);
            eventBus.unsubscribeAll(container);
            callLifecycleHook(container.getInstance(), PluginLifecycle::onDisable);
            closeClassLoader(container);
            serverLogger.info("Disabled plugin {}", container.getId());
        } catch (Exception e) {
            serverLogger.error("Error disabling plugin {}", container.getId(), e);
        }
    }

    private void closeClassLoader(PluginContainer container) {
        try {
            ((PluginClassLoader) container.getClassLoader()).close();
        } catch (IOException e) {
            serverLogger.warn("Failed to close ClassLoader for {}", container.getId());
        }
    }

    public void disableAll() {
        List<String> ids = new ArrayList<>(plugins.keySet());
        ids.forEach(this::disablePlugin);
    }

    public int getPluginCount() {
        return plugins.size();
    }

    // JSON 映射类
    private static class PluginDescription {
        String main;
        List<String> dependencies = Collections.emptyList();
    }

    // 候选记录类
    private record PluginCandidate(
            Path jar,
            PluginClassLoader loader,
            Class<?> mainClass,
            Plugin annotation,
            List<String> dependencies
    ) {
        String id() { return annotation.id(); }
        String version() { return annotation.version(); }
    }
}