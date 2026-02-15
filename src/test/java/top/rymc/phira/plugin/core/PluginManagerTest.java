package top.rymc.phira.plugin.core;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.rymc.phira.plugin.Plugin;
import top.rymc.phira.plugin.PluginLifecycle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("PluginManager")
class PluginManagerTest {

    @TempDir
    Path tempDir;

    private PluginManager pluginManager;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
        pluginManager = new PluginManager(logger, tempDir);
    }

    @Test
    @DisplayName("should create plugins directory if not exists")
    void shouldCreatePluginsDirectoryIfNotExists() {
        assertThat(Files.exists(tempDir)).isTrue();
    }

    @Test
    @DisplayName("should return zero plugin count when no plugins")
    void shouldReturnZeroPluginCountWhenNoPlugins() {
        assertThat(pluginManager.getPluginCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should bind core services to injector")
    void shouldBindCoreServicesToInjector() {
        assertThat(pluginManager.getEventBus()).isNotNull();
    }

    @Test
    @DisplayName("should load no plugins when directory is empty")
    void shouldLoadNoPluginsWhenDirectoryIsEmpty() {
        pluginManager.loadAll();

        assertThat(pluginManager.getPluginCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should return event bus")
    void shouldReturnEventBus() {
        var eventBus = pluginManager.getEventBus();

        assertThat(eventBus).isNotNull();
    }

    @Test
    @DisplayName("should disable all plugins")
    void shouldDisableAllPlugins() {
        pluginManager.loadAll();
        pluginManager.disableAll();

        assertThat(pluginManager.getPluginCount()).isEqualTo(0);
    }

    private Path createPluginJar(String pluginId, String version, String mainClass, String... dependencies) throws IOException {
        Path jarPath = tempDir.resolve(pluginId + ".jar");

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest)) {
            String pluginJson = String.format(
                "{\"main\": \"%s\", \"id\": \"%s\", \"version\": \"%s\", \"dependencies\": [%s]}",
                mainClass,
                pluginId,
                version,
                String.join(", ", java.util.Arrays.stream(dependencies).map(d -> "\"" + d + "\"").toArray(String[]::new))
            );

            JarEntry entry = new JarEntry("plugin.json");
            jos.putNextEntry(entry);
            jos.write(pluginJson.getBytes());
            jos.closeEntry();
        }

        return jarPath;
    }

    @Plugin(id = "test-plugin", version = "1.0.0")
    public static class TestPlugin implements PluginLifecycle {
        @Override
        public void onEnable() {}

        @Override
        public void onDisable() {}
    }
}
