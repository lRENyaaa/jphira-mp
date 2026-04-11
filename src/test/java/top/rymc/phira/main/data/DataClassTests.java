package top.rymc.phira.main.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DataClassTests {

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("UserInfo getters return correct values")
    void userInfoGettersReturnCorrectValues() throws Exception {
        UserInfo userInfo = new UserInfo();
        OffsetDateTime joined = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime lastLogin = OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        setField(userInfo, "id", 123);
        setField(userInfo, "name", "TestUser");
        setField(userInfo, "avatar", "avatar.png");
        setField(userInfo, "language", "en");
        setField(userInfo, "bio", "Test bio");
        setField(userInfo, "exp", 1000);
        setField(userInfo, "rks", 15.5);
        setField(userInfo, "joined", joined);
        setField(userInfo, "lastLogin", lastLogin);
        setField(userInfo, "roles", 1);
        setField(userInfo, "banned", false);
        setField(userInfo, "loginBanned", false);
        setField(userInfo, "followerCount", 100);
        setField(userInfo, "followingCount", 50);
        setField(userInfo, "email", "test@example.com");

        assertThat(userInfo.getId()).isEqualTo(123);
        assertThat(userInfo.getName()).isEqualTo("TestUser");
        assertThat(userInfo.getAvatar()).isEqualTo("avatar.png");
        assertThat(userInfo.getLanguage()).isEqualTo("en");
        assertThat(userInfo.getBio()).isEqualTo("Test bio");
        assertThat(userInfo.getExp()).isEqualTo(1000);
        assertThat(userInfo.getRks()).isEqualTo(15.5);
        assertThat(userInfo.getJoined()).isEqualTo(joined);
        assertThat(userInfo.getLastLogin()).isEqualTo(lastLogin);
        assertThat(userInfo.getRoles()).isEqualTo(1);
        assertThat(userInfo.isBanned()).isFalse();
        assertThat(userInfo.isLoginBanned()).isFalse();
        assertThat(userInfo.getFollowerCount()).isEqualTo(100);
        assertThat(userInfo.getFollowingCount()).isEqualTo(50);
        assertThat(userInfo.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("UserInfo equals returns true for same values")
    void userInfoEqualsReturnsTrueForSameValues() throws Exception {
        OffsetDateTime joined = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime lastLogin = OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        UserInfo userInfo1 = new UserInfo();
        setField(userInfo1, "id", 123);
        setField(userInfo1, "name", "TestUser");
        setField(userInfo1, "avatar", "avatar.png");
        setField(userInfo1, "language", "en");
        setField(userInfo1, "bio", "Test bio");
        setField(userInfo1, "exp", 1000);
        setField(userInfo1, "rks", 15.5);
        setField(userInfo1, "joined", joined);
        setField(userInfo1, "lastLogin", lastLogin);
        setField(userInfo1, "roles", 1);
        setField(userInfo1, "banned", false);
        setField(userInfo1, "loginBanned", false);
        setField(userInfo1, "followerCount", 100);
        setField(userInfo1, "followingCount", 50);
        setField(userInfo1, "email", "test@example.com");

        UserInfo userInfo2 = new UserInfo();
        setField(userInfo2, "id", 123);
        setField(userInfo2, "name", "TestUser");
        setField(userInfo2, "avatar", "avatar.png");
        setField(userInfo2, "language", "en");
        setField(userInfo2, "bio", "Test bio");
        setField(userInfo2, "exp", 1000);
        setField(userInfo2, "rks", 15.5);
        setField(userInfo2, "joined", joined);
        setField(userInfo2, "lastLogin", lastLogin);
        setField(userInfo2, "roles", 1);
        setField(userInfo2, "banned", false);
        setField(userInfo2, "loginBanned", false);
        setField(userInfo2, "followerCount", 100);
        setField(userInfo2, "followingCount", 50);
        setField(userInfo2, "email", "test@example.com");

        assertThat(userInfo1).isEqualTo(userInfo2);
    }

    @Test
    @DisplayName("UserInfo equals returns false for different values")
    void userInfoEqualsReturnsFalseForDifferentValues() throws Exception {
        UserInfo userInfo1 = new UserInfo();
        setField(userInfo1, "id", 123);
        setField(userInfo1, "name", "TestUser");

        UserInfo userInfo2 = new UserInfo();
        setField(userInfo2, "id", 456);
        setField(userInfo2, "name", "DifferentUser");

        assertThat(userInfo1).isNotEqualTo(userInfo2);
    }

    @Test
    @DisplayName("UserInfo hashCode returns same value for equal objects")
    void userInfoHashCodeReturnsSameValueForEqualObjects() throws Exception {
        OffsetDateTime joined = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime lastLogin = OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        UserInfo userInfo1 = new UserInfo();
        setField(userInfo1, "id", 123);
        setField(userInfo1, "name", "TestUser");
        setField(userInfo1, "avatar", "avatar.png");
        setField(userInfo1, "language", "en");
        setField(userInfo1, "bio", "Test bio");
        setField(userInfo1, "exp", 1000);
        setField(userInfo1, "rks", 15.5);
        setField(userInfo1, "joined", joined);
        setField(userInfo1, "lastLogin", lastLogin);
        setField(userInfo1, "roles", 1);
        setField(userInfo1, "banned", false);
        setField(userInfo1, "loginBanned", false);
        setField(userInfo1, "followerCount", 100);
        setField(userInfo1, "followingCount", 50);
        setField(userInfo1, "email", "test@example.com");

        UserInfo userInfo2 = new UserInfo();
        setField(userInfo2, "id", 123);
        setField(userInfo2, "name", "TestUser");
        setField(userInfo2, "avatar", "avatar.png");
        setField(userInfo2, "language", "en");
        setField(userInfo2, "bio", "Test bio");
        setField(userInfo2, "exp", 1000);
        setField(userInfo2, "rks", 15.5);
        setField(userInfo2, "joined", joined);
        setField(userInfo2, "lastLogin", lastLogin);
        setField(userInfo2, "roles", 1);
        setField(userInfo2, "banned", false);
        setField(userInfo2, "loginBanned", false);
        setField(userInfo2, "followerCount", 100);
        setField(userInfo2, "followingCount", 50);
        setField(userInfo2, "email", "test@example.com");

        assertThat(userInfo1.hashCode()).isEqualTo(userInfo2.hashCode());
    }

    @Test
    @DisplayName("UserInfo hashCode returns different value for different objects")
    void userInfoHashCodeReturnsDifferentValueForDifferentObjects() throws Exception {
        UserInfo userInfo1 = new UserInfo();
        setField(userInfo1, "id", 123);
        setField(userInfo1, "name", "TestUser");

        UserInfo userInfo2 = new UserInfo();
        setField(userInfo2, "id", 456);
        setField(userInfo2, "name", "DifferentUser");

        assertThat(userInfo1.hashCode()).isNotEqualTo(userInfo2.hashCode());
    }

    @Test
    @DisplayName("ChartInfo getters return correct values")
    void chartInfoGettersReturnCorrectValues() throws Exception {
        ChartInfo chartInfo = new ChartInfo();
        OffsetDateTime created = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime updated = OffsetDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime chartUpdated = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        String[] tags = new String[]{"tag1", "tag2"};

        setField(chartInfo, "id", 456);
        setField(chartInfo, "name", "TestChart");
        setField(chartInfo, "level", "IN");
        setField(chartInfo, "difficulty", 14.5f);
        setField(chartInfo, "charter", "CharterName");
        setField(chartInfo, "composer", "ComposerName");
        setField(chartInfo, "illustrator", "IllustratorName");
        setField(chartInfo, "description", "Test description");
        setField(chartInfo, "ranked", true);
        setField(chartInfo, "reviewed", true);
        setField(chartInfo, "stable", false);
        setField(chartInfo, "stableRequest", true);
        setField(chartInfo, "illustration", "illustration.png");
        setField(chartInfo, "preview", "preview.mp3");
        setField(chartInfo, "file", "chart.json");
        setField(chartInfo, "uploader", 789);
        setField(chartInfo, "tags", tags);
        setField(chartInfo, "rating", 4.5f);
        setField(chartInfo, "ratingCount", 100);
        setField(chartInfo, "created", created);
        setField(chartInfo, "updated", updated);
        setField(chartInfo, "chartUpdated", chartUpdated);

        assertThat(chartInfo.getId()).isEqualTo(456);
        assertThat(chartInfo.getName()).isEqualTo("TestChart");
        assertThat(chartInfo.getLevel()).isEqualTo("IN");
        assertThat(chartInfo.getDifficulty()).isEqualTo(14.5f);
        assertThat(chartInfo.getCharter()).isEqualTo("CharterName");
        assertThat(chartInfo.getComposer()).isEqualTo("ComposerName");
        assertThat(chartInfo.getIllustrator()).isEqualTo("IllustratorName");
        assertThat(chartInfo.getDescription()).isEqualTo("Test description");
        assertThat(chartInfo.isRanked()).isTrue();
        assertThat(chartInfo.isReviewed()).isTrue();
        assertThat(chartInfo.isStable()).isFalse();
        assertThat(chartInfo.isStableRequest()).isTrue();
        assertThat(chartInfo.getIllustration()).isEqualTo("illustration.png");
        assertThat(chartInfo.getPreview()).isEqualTo("preview.mp3");
        assertThat(chartInfo.getFile()).isEqualTo("chart.json");
        assertThat(chartInfo.getUploader()).isEqualTo(789);
        assertThat(chartInfo.getTags()).isEqualTo(tags);
        assertThat(chartInfo.getRating()).isEqualTo(4.5f);
        assertThat(chartInfo.getRatingCount()).isEqualTo(100);
        assertThat(chartInfo.getCreated()).isEqualTo(created);
        assertThat(chartInfo.getUpdated()).isEqualTo(updated);
        assertThat(chartInfo.getChartUpdated()).isEqualTo(chartUpdated);
    }

    @Test
    @DisplayName("ChartInfo equals returns true for same values")
    void chartInfoEqualsReturnsTrueForSameValues() throws Exception {
        OffsetDateTime created = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime updated = OffsetDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime chartUpdated = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        String[] tags = new String[]{"tag1", "tag2"};

        ChartInfo chartInfo1 = new ChartInfo();
        setField(chartInfo1, "id", 456);
        setField(chartInfo1, "name", "TestChart");
        setField(chartInfo1, "level", "IN");
        setField(chartInfo1, "difficulty", 14.5f);
        setField(chartInfo1, "charter", "CharterName");
        setField(chartInfo1, "composer", "ComposerName");
        setField(chartInfo1, "illustrator", "IllustratorName");
        setField(chartInfo1, "description", "Test description");
        setField(chartInfo1, "ranked", true);
        setField(chartInfo1, "reviewed", true);
        setField(chartInfo1, "stable", false);
        setField(chartInfo1, "stableRequest", true);
        setField(chartInfo1, "illustration", "illustration.png");
        setField(chartInfo1, "preview", "preview.mp3");
        setField(chartInfo1, "file", "chart.json");
        setField(chartInfo1, "uploader", 789);
        setField(chartInfo1, "tags", tags);
        setField(chartInfo1, "rating", 4.5f);
        setField(chartInfo1, "ratingCount", 100);
        setField(chartInfo1, "created", created);
        setField(chartInfo1, "updated", updated);
        setField(chartInfo1, "chartUpdated", chartUpdated);

        ChartInfo chartInfo2 = new ChartInfo();
        setField(chartInfo2, "id", 456);
        setField(chartInfo2, "name", "TestChart");
        setField(chartInfo2, "level", "IN");
        setField(chartInfo2, "difficulty", 14.5f);
        setField(chartInfo2, "charter", "CharterName");
        setField(chartInfo2, "composer", "ComposerName");
        setField(chartInfo2, "illustrator", "IllustratorName");
        setField(chartInfo2, "description", "Test description");
        setField(chartInfo2, "ranked", true);
        setField(chartInfo2, "reviewed", true);
        setField(chartInfo2, "stable", false);
        setField(chartInfo2, "stableRequest", true);
        setField(chartInfo2, "illustration", "illustration.png");
        setField(chartInfo2, "preview", "preview.mp3");
        setField(chartInfo2, "file", "chart.json");
        setField(chartInfo2, "uploader", 789);
        setField(chartInfo2, "tags", tags);
        setField(chartInfo2, "rating", 4.5f);
        setField(chartInfo2, "ratingCount", 100);
        setField(chartInfo2, "created", created);
        setField(chartInfo2, "updated", updated);
        setField(chartInfo2, "chartUpdated", chartUpdated);

        assertThat(chartInfo1).isEqualTo(chartInfo2);
    }

    @Test
    @DisplayName("ChartInfo equals returns false for different values")
    void chartInfoEqualsReturnsFalseForDifferentValues() throws Exception {
        ChartInfo chartInfo1 = new ChartInfo();
        setField(chartInfo1, "id", 456);
        setField(chartInfo1, "name", "TestChart");

        ChartInfo chartInfo2 = new ChartInfo();
        setField(chartInfo2, "id", 789);
        setField(chartInfo2, "name", "DifferentChart");

        assertThat(chartInfo1).isNotEqualTo(chartInfo2);
    }

    @Test
    @DisplayName("ChartInfo hashCode returns same value for equal objects")
    void chartInfoHashCodeReturnsSameValueForEqualObjects() throws Exception {
        OffsetDateTime created = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime updated = OffsetDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime chartUpdated = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        String[] tags = new String[]{"tag1", "tag2"};

        ChartInfo chartInfo1 = new ChartInfo();
        setField(chartInfo1, "id", 456);
        setField(chartInfo1, "name", "TestChart");
        setField(chartInfo1, "level", "IN");
        setField(chartInfo1, "difficulty", 14.5f);
        setField(chartInfo1, "charter", "CharterName");
        setField(chartInfo1, "composer", "ComposerName");
        setField(chartInfo1, "illustrator", "IllustratorName");
        setField(chartInfo1, "description", "Test description");
        setField(chartInfo1, "ranked", true);
        setField(chartInfo1, "reviewed", true);
        setField(chartInfo1, "stable", false);
        setField(chartInfo1, "stableRequest", true);
        setField(chartInfo1, "illustration", "illustration.png");
        setField(chartInfo1, "preview", "preview.mp3");
        setField(chartInfo1, "file", "chart.json");
        setField(chartInfo1, "uploader", 789);
        setField(chartInfo1, "tags", tags);
        setField(chartInfo1, "rating", 4.5f);
        setField(chartInfo1, "ratingCount", 100);
        setField(chartInfo1, "created", created);
        setField(chartInfo1, "updated", updated);
        setField(chartInfo1, "chartUpdated", chartUpdated);

        ChartInfo chartInfo2 = new ChartInfo();
        setField(chartInfo2, "id", 456);
        setField(chartInfo2, "name", "TestChart");
        setField(chartInfo2, "level", "IN");
        setField(chartInfo2, "difficulty", 14.5f);
        setField(chartInfo2, "charter", "CharterName");
        setField(chartInfo2, "composer", "ComposerName");
        setField(chartInfo2, "illustrator", "IllustratorName");
        setField(chartInfo2, "description", "Test description");
        setField(chartInfo2, "ranked", true);
        setField(chartInfo2, "reviewed", true);
        setField(chartInfo2, "stable", false);
        setField(chartInfo2, "stableRequest", true);
        setField(chartInfo2, "illustration", "illustration.png");
        setField(chartInfo2, "preview", "preview.mp3");
        setField(chartInfo2, "file", "chart.json");
        setField(chartInfo2, "uploader", 789);
        setField(chartInfo2, "tags", tags);
        setField(chartInfo2, "rating", 4.5f);
        setField(chartInfo2, "ratingCount", 100);
        setField(chartInfo2, "created", created);
        setField(chartInfo2, "updated", updated);
        setField(chartInfo2, "chartUpdated", chartUpdated);

        assertThat(chartInfo1.hashCode()).isEqualTo(chartInfo2.hashCode());
    }

    @Test
    @DisplayName("ChartInfo hashCode returns different value for different objects")
    void chartInfoHashCodeReturnsDifferentValueForDifferentObjects() throws Exception {
        ChartInfo chartInfo1 = new ChartInfo();
        setField(chartInfo1, "id", 456);
        setField(chartInfo1, "name", "TestChart");

        ChartInfo chartInfo2 = new ChartInfo();
        setField(chartInfo2, "id", 789);
        setField(chartInfo2, "name", "DifferentChart");

        assertThat(chartInfo1.hashCode()).isNotEqualTo(chartInfo2.hashCode());
    }

    @Test
    @DisplayName("GameRecord getters return correct values")
    void gameRecordGettersReturnCorrectValues() {
        OffsetDateTime time = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        GameRecord gameRecord = new GameRecord();
        gameRecord.setId(100);
        gameRecord.setPlayer(123);
        gameRecord.setChart(456);
        gameRecord.setScore(1000000);
        gameRecord.setAccuracy(100.0f);
        gameRecord.setPerfect(1000);
        gameRecord.setGood(0);
        gameRecord.setBad(0);
        gameRecord.setMiss(0);
        gameRecord.setSpeed(1.0f);
        gameRecord.setMaxCombo(1000);
        gameRecord.setBest(true);
        gameRecord.setBestStd(true);
        gameRecord.setMods(0);
        gameRecord.setFullCombo(true);
        gameRecord.setTime(time);
        gameRecord.setStd(15.0f);
        gameRecord.setStdScore(1000000.0f);

        assertThat(gameRecord.getId()).isEqualTo(100);
        assertThat(gameRecord.getPlayer()).isEqualTo(123);
        assertThat(gameRecord.getChart()).isEqualTo(456);
        assertThat(gameRecord.getScore()).isEqualTo(1000000);
        assertThat(gameRecord.getAccuracy()).isEqualTo(100.0f);
        assertThat(gameRecord.getPerfect()).isEqualTo(1000);
        assertThat(gameRecord.getGood()).isEqualTo(0);
        assertThat(gameRecord.getBad()).isEqualTo(0);
        assertThat(gameRecord.getMiss()).isEqualTo(0);
        assertThat(gameRecord.getSpeed()).isEqualTo(1.0f);
        assertThat(gameRecord.getMaxCombo()).isEqualTo(1000);
        assertThat(gameRecord.isBest()).isTrue();
        assertThat(gameRecord.isBestStd()).isTrue();
        assertThat(gameRecord.getMods()).isEqualTo(0);
        assertThat(gameRecord.isFullCombo()).isTrue();
        assertThat(gameRecord.getTime()).isEqualTo(time);
        assertThat(gameRecord.getStd()).isEqualTo(15.0f);
        assertThat(gameRecord.getStdScore()).isEqualTo(1000000.0f);
    }

    @Test
    @DisplayName("GameRecord equals returns true for same values")
    void gameRecordEqualsReturnsTrueForSameValues() {
        OffsetDateTime time = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        GameRecord gameRecord1 = new GameRecord();
        gameRecord1.setId(100);
        gameRecord1.setPlayer(123);
        gameRecord1.setChart(456);
        gameRecord1.setScore(1000000);
        gameRecord1.setAccuracy(100.0f);
        gameRecord1.setPerfect(1000);
        gameRecord1.setGood(0);
        gameRecord1.setBad(0);
        gameRecord1.setMiss(0);
        gameRecord1.setSpeed(1.0f);
        gameRecord1.setMaxCombo(1000);
        gameRecord1.setBest(true);
        gameRecord1.setBestStd(true);
        gameRecord1.setMods(0);
        gameRecord1.setFullCombo(true);
        gameRecord1.setTime(time);
        gameRecord1.setStd(15.0f);
        gameRecord1.setStdScore(1000000.0f);

        GameRecord gameRecord2 = new GameRecord();
        gameRecord2.setId(100);
        gameRecord2.setPlayer(123);
        gameRecord2.setChart(456);
        gameRecord2.setScore(1000000);
        gameRecord2.setAccuracy(100.0f);
        gameRecord2.setPerfect(1000);
        gameRecord2.setGood(0);
        gameRecord2.setBad(0);
        gameRecord2.setMiss(0);
        gameRecord2.setSpeed(1.0f);
        gameRecord2.setMaxCombo(1000);
        gameRecord2.setBest(true);
        gameRecord2.setBestStd(true);
        gameRecord2.setMods(0);
        gameRecord2.setFullCombo(true);
        gameRecord2.setTime(time);
        gameRecord2.setStd(15.0f);
        gameRecord2.setStdScore(1000000.0f);

        assertThat(gameRecord1).isEqualTo(gameRecord2);
    }

    @Test
    @DisplayName("GameRecord equals returns false for different values")
    void gameRecordEqualsReturnsFalseForDifferentValues() {
        GameRecord gameRecord1 = new GameRecord();
        gameRecord1.setId(100);
        gameRecord1.setScore(1000000);

        GameRecord gameRecord2 = new GameRecord();
        gameRecord2.setId(200);
        gameRecord2.setScore(900000);

        assertThat(gameRecord1).isNotEqualTo(gameRecord2);
    }

    @Test
    @DisplayName("GameRecord hashCode returns same value for equal objects")
    void gameRecordHashCodeReturnsSameValueForEqualObjects() {
        OffsetDateTime time = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        GameRecord gameRecord1 = new GameRecord();
        gameRecord1.setId(100);
        gameRecord1.setPlayer(123);
        gameRecord1.setChart(456);
        gameRecord1.setScore(1000000);
        gameRecord1.setAccuracy(100.0f);
        gameRecord1.setPerfect(1000);
        gameRecord1.setGood(0);
        gameRecord1.setBad(0);
        gameRecord1.setMiss(0);
        gameRecord1.setSpeed(1.0f);
        gameRecord1.setMaxCombo(1000);
        gameRecord1.setBest(true);
        gameRecord1.setBestStd(true);
        gameRecord1.setMods(0);
        gameRecord1.setFullCombo(true);
        gameRecord1.setTime(time);
        gameRecord1.setStd(15.0f);
        gameRecord1.setStdScore(1000000.0f);

        GameRecord gameRecord2 = new GameRecord();
        gameRecord2.setId(100);
        gameRecord2.setPlayer(123);
        gameRecord2.setChart(456);
        gameRecord2.setScore(1000000);
        gameRecord2.setAccuracy(100.0f);
        gameRecord2.setPerfect(1000);
        gameRecord2.setGood(0);
        gameRecord2.setBad(0);
        gameRecord2.setMiss(0);
        gameRecord2.setSpeed(1.0f);
        gameRecord2.setMaxCombo(1000);
        gameRecord2.setBest(true);
        gameRecord2.setBestStd(true);
        gameRecord2.setMods(0);
        gameRecord2.setFullCombo(true);
        gameRecord2.setTime(time);
        gameRecord2.setStd(15.0f);
        gameRecord2.setStdScore(1000000.0f);

        assertThat(gameRecord1.hashCode()).isEqualTo(gameRecord2.hashCode());
    }

    @Test
    @DisplayName("GameRecord hashCode returns different value for different objects")
    void gameRecordHashCodeReturnsDifferentValueForDifferentObjects() {
        GameRecord gameRecord1 = new GameRecord();
        gameRecord1.setId(100);
        gameRecord1.setScore(1000000);

        GameRecord gameRecord2 = new GameRecord();
        gameRecord2.setId(200);
        gameRecord2.setScore(900000);

        assertThat(gameRecord1.hashCode()).isNotEqualTo(gameRecord2.hashCode());
    }

    @Test
    @DisplayName("GameRecord all args constructor works correctly")
    void gameRecordAllArgsConstructorWorksCorrectly() {
        OffsetDateTime time = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        GameRecord gameRecord = new GameRecord(
                100, 123, 456, 1000000, 100.0f,
                1000, 0, 0, 0, 1.0f, 1000,
                true, true, 0, true, time, 15.0f, 1000000.0f
        );

        assertThat(gameRecord.getId()).isEqualTo(100);
        assertThat(gameRecord.getPlayer()).isEqualTo(123);
        assertThat(gameRecord.getChart()).isEqualTo(456);
        assertThat(gameRecord.getScore()).isEqualTo(1000000);
        assertThat(gameRecord.getAccuracy()).isEqualTo(100.0f);
        assertThat(gameRecord.getPerfect()).isEqualTo(1000);
        assertThat(gameRecord.getGood()).isEqualTo(0);
        assertThat(gameRecord.getBad()).isEqualTo(0);
        assertThat(gameRecord.getMiss()).isEqualTo(0);
        assertThat(gameRecord.getSpeed()).isEqualTo(1.0f);
        assertThat(gameRecord.getMaxCombo()).isEqualTo(1000);
        assertThat(gameRecord.isBest()).isTrue();
        assertThat(gameRecord.isBestStd()).isTrue();
        assertThat(gameRecord.getMods()).isEqualTo(0);
        assertThat(gameRecord.isFullCombo()).isTrue();
        assertThat(gameRecord.getTime()).isEqualTo(time);
        assertThat(gameRecord.getStd()).isEqualTo(15.0f);
        assertThat(gameRecord.getStdScore()).isEqualTo(1000000.0f);
    }

    @Test
    @DisplayName("GameRecord no args constructor creates empty object")
    void gameRecordNoArgsConstructorCreatesEmptyObject() {
        GameRecord gameRecord = new GameRecord();

        assertThat(gameRecord.getId()).isEqualTo(0);
        assertThat(gameRecord.getPlayer()).isEqualTo(0);
        assertThat(gameRecord.getChart()).isEqualTo(0);
        assertThat(gameRecord.getScore()).isEqualTo(0);
        assertThat(gameRecord.getAccuracy()).isEqualTo(0.0f);
        assertThat(gameRecord.getPerfect()).isEqualTo(0);
        assertThat(gameRecord.getGood()).isEqualTo(0);
        assertThat(gameRecord.getBad()).isEqualTo(0);
        assertThat(gameRecord.getMiss()).isEqualTo(0);
        assertThat(gameRecord.getSpeed()).isEqualTo(0.0f);
        assertThat(gameRecord.getMaxCombo()).isEqualTo(0);
        assertThat(gameRecord.isBest()).isFalse();
        assertThat(gameRecord.isBestStd()).isFalse();
        assertThat(gameRecord.getMods()).isEqualTo(0);
        assertThat(gameRecord.isFullCombo()).isFalse();
        assertThat(gameRecord.getTime()).isNull();
        assertThat(gameRecord.getStd()).isEqualTo(0.0f);
        assertThat(gameRecord.getStdScore()).isEqualTo(0.0f);
    }
}
