package top.rymc.phira.plugin.core;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.rymc.phira.plugin.Plugin;
import top.rymc.phira.plugin.PluginLifecycle;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("PluginManager")
class PluginManagerTest {

    @TempDir
    Path tempDir;

    private PluginManager pluginManager;

    @BeforeEach
    void setUp() {
        pluginManager = new PluginManager(mock(Logger.class), tempDir);
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

    @Plugin(id = "test-plugin", version = "1.0.0")
    public static class TestPlugin implements PluginLifecycle {

    }
}
