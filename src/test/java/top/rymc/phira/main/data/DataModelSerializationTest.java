package top.rymc.phira.main.data;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.game.ReflectionUtil;
import top.rymc.phira.main.util.GsonUtil;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Data Model Serialization")
class DataModelSerializationTest {

    private final Gson gson = GsonUtil.getGson();

    @Test
    @DisplayName("should serialize and deserialize UserInfo")
    void shouldSerializeAndDeserializeUserInfo() {
        var userInfo = createUserInfo(1, "testuser", "Test User", "test@example.com");

        var json = gson.toJson(userInfo);
        var deserialized = gson.fromJson(json, UserInfo.class);

        assertThat(deserialized.getId()).isEqualTo(userInfo.getId());
        assertThat(deserialized.getName()).isEqualTo(userInfo.getName());
        assertThat(deserialized.getEmail()).isEqualTo(userInfo.getEmail());
    }

    @Test
    @DisplayName("should serialize UserInfo with underscore naming")
    void shouldSerializeUserInfoWithUnderscoreNaming() {
        var userInfo = createUserInfo(1, "testuser", "Test User", "test@example.com");

        var json = gson.toJson(userInfo);

        assertThat(json).contains("\"follower_count\"");
        assertThat(json).contains("\"following_count\"");
    }

    @Test
    @DisplayName("should serialize and deserialize ChartInfo")
    void shouldSerializeAndDeserializeChartInfo() {
        var chartInfo = createChartInfo(42, "Test Chart", "Test Charter", 5);

        var json = gson.toJson(chartInfo);
        var deserialized = gson.fromJson(json, ChartInfo.class);

        assertThat(deserialized.getId()).isEqualTo(chartInfo.getId());
        assertThat(deserialized.getName()).isEqualTo(chartInfo.getName());
        assertThat(deserialized.getCharter()).isEqualTo(chartInfo.getCharter());
    }

    @Test
    @DisplayName("should serialize ChartInfo with underscore naming")
    void shouldSerializeChartInfoWithUnderscoreNaming() {
        var chartInfo = createChartInfo(42, "Test Chart", "Test Charter", 5);

        var json = gson.toJson(chartInfo);

        assertThat(json).contains("\"stable_request\"");
        assertThat(json).contains("\"rating_count\"");
    }

    @Test
    @DisplayName("should serialize and deserialize GameRecord")
    void shouldSerializeAndDeserializeGameRecord() {
        var record = createGameRecord(1001, 1, 42, 1000000, 100.0f, true);

        var json = gson.toJson(record);
        var deserialized = gson.fromJson(json, GameRecord.class);

        assertThat(deserialized.getId()).isEqualTo(record.getId());
        assertThat(deserialized.getScore()).isEqualTo(record.getScore());
        assertThat(deserialized.getAccuracy()).isEqualTo(record.getAccuracy());
        assertThat(deserialized.isFullCombo()).isEqualTo(record.isFullCombo());
    }

    @Test
    @DisplayName("should serialize GameRecord with underscore naming")
    void shouldSerializeGameRecordWithUnderscoreNaming() {
        var record = createGameRecord(1001, 1, 42, 1000000, 100.0f, true);

        var json = gson.toJson(record);

        assertThat(json).contains("\"full_combo\"");
        assertThat(json).contains("\"max_combo\"");
        assertThat(json).contains("\"std_score\"");
    }

    @Test
    @DisplayName("should handle null values in data models")
    void shouldHandleNullValuesInDataModels() {
        var userInfo = new UserInfo();

        var json = gson.toJson(userInfo);
        var deserialized = gson.fromJson(json, UserInfo.class);

        assertThat(deserialized).isNotNull();
    }

    @Test
    @DisplayName("should handle OffsetDateTime in UserInfo")
    void shouldHandleOffsetDateTimeInUserInfo() {
        var userInfo = createUserInfo(1, "testuser", "Test User", "test@example.com");
        ReflectionUtil.setField(userInfo, "joined", OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC));
        ReflectionUtil.setField(userInfo, "lastLogin", OffsetDateTime.of(2024, 2, 20, 15, 45, 0, 0, ZoneOffset.UTC));

        var json = gson.toJson(userInfo);
        var deserialized = gson.fromJson(json, UserInfo.class);

        assertThat(deserialized.getJoined()).isEqualTo(userInfo.getJoined());
        assertThat(deserialized.getLastLogin()).isEqualTo(userInfo.getLastLogin());
    }

    private UserInfo createUserInfo(int id, String name, String bio, String email) {
        var userInfo = new UserInfo();
        ReflectionUtil.setField(userInfo, "id", id);
        ReflectionUtil.setField(userInfo, "name", name);
        ReflectionUtil.setField(userInfo, "bio", bio);
        ReflectionUtil.setField(userInfo, "email", email);
        ReflectionUtil.setField(userInfo, "exp", 100);
        ReflectionUtil.setField(userInfo, "rks", 15.5);
        ReflectionUtil.setField(userInfo, "roles", 1);
        ReflectionUtil.setField(userInfo, "banned", false);
        ReflectionUtil.setField(userInfo, "loginBanned", false);
        ReflectionUtil.setField(userInfo, "followerCount", 50);
        ReflectionUtil.setField(userInfo, "followingCount", 30);
        return userInfo;
    }

    private ChartInfo createChartInfo(int id, String name, String charter, int level) {
        var chartInfo = new ChartInfo();
        ReflectionUtil.setField(chartInfo, "id", id);
        ReflectionUtil.setField(chartInfo, "name", name);
        ReflectionUtil.setField(chartInfo, "charter", charter);
        ReflectionUtil.setField(chartInfo, "level", String.valueOf(level));
        ReflectionUtil.setField(chartInfo, "difficulty", 3.0f);
        ReflectionUtil.setField(chartInfo, "composer", "Test Composer");
        ReflectionUtil.setField(chartInfo, "illustrator", "Test Illustrator");
        ReflectionUtil.setField(chartInfo, "ranked", true);
        ReflectionUtil.setField(chartInfo, "reviewed", true);
        ReflectionUtil.setField(chartInfo, "stable", true);
        ReflectionUtil.setField(chartInfo, "stableRequest", false);
        ReflectionUtil.setField(chartInfo, "rating", 4.5f);
        ReflectionUtil.setField(chartInfo, "ratingCount", 100);
        return chartInfo;
    }

    private GameRecord createGameRecord(int id, int playerId, int chartId, int score, float accuracy, boolean fullCombo) {
        var record = new GameRecord();
        ReflectionUtil.setField(record, "id", id);
        ReflectionUtil.setField(record, "player", playerId);
        ReflectionUtil.setField(record, "chart", chartId);
        ReflectionUtil.setField(record, "score", score);
        ReflectionUtil.setField(record, "accuracy", accuracy);
        ReflectionUtil.setField(record, "perfect", 500);
        ReflectionUtil.setField(record, "good", 10);
        ReflectionUtil.setField(record, "bad", 0);
        ReflectionUtil.setField(record, "miss", 0);
        ReflectionUtil.setField(record, "speed", 1.0f);
        ReflectionUtil.setField(record, "maxCombo", 510);
        ReflectionUtil.setField(record, "fullCombo", fullCombo);
        ReflectionUtil.setField(record, "best", true);
        ReflectionUtil.setField(record, "bestStd", true);
        return record;
    }
}
