package top.rymc.phira.test;

import top.rymc.phira.main.Server;
import top.rymc.phira.plugin.core.PluginEventBus;
import top.rymc.phira.plugin.core.PluginManager;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestServerSetup {

    private static boolean initialized = false;

    public static synchronized void init() throws Exception {
        if (initialized) {
            return;
        }

        Server server = Server.getInstance();

        Path tempDir = Files.createTempDirectory("phira-test-plugins");

        Field pluginManagerField = Server.class.getDeclaredField("pluginManager");
        pluginManagerField.setAccessible(true);

        PluginManager pluginManager = new PluginManager(Server.getLogger(), tempDir);
        pluginManagerField.set(server, pluginManager);

        initialized = true;
    }
}
