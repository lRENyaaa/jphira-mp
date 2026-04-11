package top.rymc.phira.main.game.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.player.Player;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class I18nServiceTest {

    @Mock
    private Player player;

    @Mock
    private UserInfo userInfo;

    private static final String DEFAULT_LANGUAGE = "zh-CN";
    private static final String ENGLISH_LANGUAGE = "en-US";
    private static final String NON_EXISTENT_LANGUAGE = "fr-FR";

    @BeforeEach
    void setUp() throws Exception {
        clearLoadedLanguages();
        I18nService.INSTANCE.setDefaultLanguage(DEFAULT_LANGUAGE);
    }

    @AfterEach
    void tearDown() throws Exception {
        clearLoadedLanguages();
        I18nService.INSTANCE.setDefaultLanguage(DEFAULT_LANGUAGE);
    }

    private void clearLoadedLanguages() throws Exception {
        Field loadedLanguagesField = I18nService.class.getDeclaredField("loadedLanguages");
        loadedLanguagesField.setAccessible(true);
        Map<?, ?> loadedLanguages = (Map<?, ?>) loadedLanguagesField.get(I18nService.INSTANCE);
        loadedLanguages.clear();
    }

    @Test
    @DisplayName("getMessage with key uses default language")
    void getMessageWithKeyUsesDefaultLanguage() {
        String key = "error.permission_denied";

        String result = I18nService.INSTANCE.getMessage(key);

        assertThat(result).isEqualTo("你没有权限");
    }

    @Test
    @DisplayName("getMessage with language and key uses specified language")
    void getMessageWithLanguageAndKeyUsesSpecifiedLanguage() {
        String key = "error.permission_denied";

        String result = I18nService.INSTANCE.getMessage(ENGLISH_LANGUAGE, key);

        assertThat(result).isEqualTo("Permission denied");
    }

    @Test
    @DisplayName("getMessage with player and key uses player language preference")
    void getMessageWithPlayerAndKeyUsesPlayerLanguagePreference() {
        String key = "error.permission_denied";
        when(player.getLanguage()).thenReturn(ENGLISH_LANGUAGE);

        String result = I18nService.INSTANCE.getMessage(player, key);

        assertThat(result).isEqualTo("Permission denied");
    }

    @Test
    @DisplayName("getMessage returns key when translation not found")
    void getMessageReturnsKeyWhenTranslationNotFound() {
        String nonExistentKey = "non.existent.key";

        String result = I18nService.INSTANCE.getMessage(nonExistentKey);

        assertThat(result).isEqualTo(nonExistentKey);
    }

    @Test
    @DisplayName("getMessage falls back to default language when specified language file not found")
    void getMessageFallsBackToDefaultLanguageWhenSpecifiedLanguageFileNotFound() throws Exception {
        String key = "error.permission_denied";

        preloadDefaultLanguage();

        String result = I18nService.INSTANCE.getMessage(NON_EXISTENT_LANGUAGE, key);

        assertThat(result).isEqualTo("你没有权限");
    }

    @Test
    @DisplayName("getMessage with player falls back to default when player language file not found")
    void getMessageWithPlayerFallsBackToDefaultWhenPlayerLanguageFileNotFound() throws Exception {
        String key = "error.permission_denied";
        when(player.getLanguage()).thenReturn(NON_EXISTENT_LANGUAGE);

        preloadDefaultLanguage();

        String result = I18nService.INSTANCE.getMessage(player, key);

        assertThat(result).isEqualTo("你没有权限");
    }

    private void preloadDefaultLanguage() {
        I18nService.INSTANCE.getMessage(DEFAULT_LANGUAGE, "error.permission_denied");
    }

    @Test
    @DisplayName("getMessage with null player uses default language")
    void getMessageWithNullPlayerUsesDefaultLanguage() {
        String key = "error.permission_denied";

        String result = I18nService.INSTANCE.getMessage((Player) null, key);

        assertThat(result).isEqualTo("你没有权限");
    }

    @Test
    @DisplayName("getMessage with different keys returns correct translations")
    void getMessageWithDifferentKeysReturnsCorrectTranslations() {
        assertThat(I18nService.INSTANCE.getMessage(ENGLISH_LANGUAGE, "error.room_full"))
                .isEqualTo("Room is full");
        assertThat(I18nService.INSTANCE.getMessage(ENGLISH_LANGUAGE, "error.room_not_found"))
                .isEqualTo("Room not found");
        assertThat(I18nService.INSTANCE.getMessage(ENGLISH_LANGUAGE, "error.not_in_room"))
                .isEqualTo("You are not in a room");
    }

    @Test
    @DisplayName("getMessage caches loaded languages")
    @SuppressWarnings("unchecked")
    void getMessageCachesLoadedLanguages() throws Exception {
        String key = "error.permission_denied";

        I18nService.INSTANCE.getMessage(ENGLISH_LANGUAGE, key);

        Field loadedLanguagesField = I18nService.class.getDeclaredField("loadedLanguages");
        loadedLanguagesField.setAccessible(true);
        Map<String, ?> loadedLanguages = (Map<String, ?>) loadedLanguagesField.get(I18nService.INSTANCE);

        assertThat(loadedLanguages).containsKey(ENGLISH_LANGUAGE);
    }

    @Test
    @DisplayName("setDefaultLanguage changes default language for getMessage with key")
    void setDefaultLanguageChangesDefaultLanguageForGetMessageWithKey() {
        String key = "error.permission_denied";
        I18nService.INSTANCE.setDefaultLanguage(ENGLISH_LANGUAGE);

        String result = I18nService.INSTANCE.getMessage(key);

        assertThat(result).isEqualTo("Permission denied");
    }

    @Test
    @DisplayName("setDefaultLanguage with null falls back to zh-CN")
    void setDefaultLanguageWithNullFallsBackToZhCn() {
        String key = "error.permission_denied";
        I18nService.INSTANCE.setDefaultLanguage(null);

        String result = I18nService.INSTANCE.getMessage(key);

        assertThat(result).isEqualTo("你没有权限");
    }

    @Test
    @DisplayName("getMessage returns key for empty string key")
    void getMessageReturnsKeyForEmptyStringKey() {
        String emptyKey = "";

        String result = I18nService.INSTANCE.getMessage(emptyKey);

        assertThat(result).isEqualTo(emptyKey);
    }

    @Test
    @DisplayName("getMessage with system key returns correct translation")
    void getMessageWithSystemKeyReturnsCorrectTranslation() {
        String key = "system.live_recorder_name";

        String englishResult = I18nService.INSTANCE.getMessage(ENGLISH_LANGUAGE, key);
        String chineseResult = I18nService.INSTANCE.getMessage(DEFAULT_LANGUAGE, key);

        assertThat(englishResult).isEqualTo("Live Recorder (Please ignore this account)");
        assertThat(chineseResult).isEqualTo("录制状态设置器(请忽略该账号)");
    }
}
