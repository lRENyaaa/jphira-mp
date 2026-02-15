package top.rymc.phira.main.game.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.game.player.Player;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class I18nService {

    private static final String LANG_RESOURCE_PATH = "/lang/";
    private static final String DEFAULT_LANGUAGE = "zh-CN";
    private static final Gson GSON = new Gson();

    public static final I18nService INSTANCE = new I18nService();

    private final Map<String, Map<String, String>> loadedLanguages = new HashMap<>();
    private String serverDefaultLanguage = DEFAULT_LANGUAGE;

    private I18nService() {}

    public void setDefaultLanguage(String language) {
        this.serverDefaultLanguage = language != null ? language : DEFAULT_LANGUAGE;
    }

    public String getMessage(String key) {
        return getMessage(serverDefaultLanguage, key);
    }

    public String getMessage(Player player, String key) {
        String language = player != null ? player.getLanguage() : serverDefaultLanguage;
        return getMessage(language, key);
    }

    public String getMessage(String language, String key) {
        Map<String, String> langMap = loadLanguage(language);
        String message = langMap.get(key);
        return message != null ? message : key;
    }

    private Map<String, String> loadLanguage(String language) {
        return loadedLanguages.computeIfAbsent(language, this::loadLanguageFile);
    }

    private Map<String, String> loadLanguageFile(String language) {
        String resourcePath = LANG_RESOURCE_PATH + language + ".json";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                return GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), new TypeToken<Map<String, String>>() {}.getType());
            }

            Server.getLogger().warn("Language file not found: {}, falling back to default", language);
            if (!language.equals(DEFAULT_LANGUAGE)) {
                return loadLanguage(DEFAULT_LANGUAGE);
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            Server.getLogger().error("Failed to load language file: {}", language, e);
            return Collections.emptyMap();
        }
    }
}
