package top.rymc.phira.main.util;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GsonUtil")
class GsonUtilTest {

    private final Gson gson = GsonUtil.getGson();

    @Test
    @DisplayName("should serialize OffsetDateTime to ISO string")
    void shouldSerializeOffsetDateTimeToIsoString() {
        var dateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        var obj = new TestObject(dateTime);

        var json = gson.toJson(obj);

        assertThat(json).contains("2024-01-15T10:30Z");
    }

    @Test
    @DisplayName("should deserialize ISO string to OffsetDateTime")
    void shouldDeserializeIsoStringToOffsetDateTime() {
        var json = "{\"timestamp\":\"2024-01-15T10:30:00Z\"}";

        var obj = gson.fromJson(json, TestObject.class);

        assertThat(obj.getTimestamp()).isEqualTo(OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("should use underscore naming policy")
    void shouldUseUnderscoreNamingPolicy() {
        var obj = new SnakeCaseObject("value");

        var json = gson.toJson(obj);

        assertThat(json).contains("\"field_name\"");
        assertThat(json).doesNotContain("\"fieldName\"");
    }

    @Test
    @DisplayName("should deserialize underscore to camelCase")
    void shouldDeserializeUnderscoreToCamelCase() {
        var json = "{\"field_name\":\"value\"}";

        var obj = gson.fromJson(json, SnakeCaseObject.class);

        assertThat(obj.getFieldName()).isEqualTo("value");
    }

    @Test
    @DisplayName("should return same gson instance")
    void shouldReturnSameGsonInstance() {
        var first = GsonUtil.getGson();
        var second = GsonUtil.getGson();

        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("should handle null OffsetDateTime")
    void shouldHandleNullOffsetDateTime() {
        var obj = new TestObject(null);

        var json = gson.toJson(obj);

        assertThat(json).isEqualTo("{}");
    }

    @Test
    @DisplayName("should handle different time zones")
    void shouldHandleDifferentTimeZones() {
        var dateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.ofHours(8));
        var obj = new TestObject(dateTime);

        var json = gson.toJson(obj);
        var deserialized = gson.fromJson(json, TestObject.class);

        assertThat(deserialized.getTimestamp()).isEqualTo(dateTime);
    }

    @Test
    @DisplayName("should parse valid JSON")
    void shouldParseValidJson() {
        var json = "{\"timestamp\":\"2024-01-15T10:30:00Z\"}";

        var obj = gson.fromJson(json, TestObject.class);

        assertThat(obj.getTimestamp()).isNotNull();
    }

    private static class TestObject {
        private OffsetDateTime timestamp;

        TestObject() {}

        TestObject(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }

        OffsetDateTime getTimestamp() {
            return timestamp;
        }
    }

    private static class SnakeCaseObject {
        private String fieldName;

        SnakeCaseObject() {}

        SnakeCaseObject(String fieldName) {
            this.fieldName = fieldName;
        }

        String getFieldName() {
            return fieldName;
        }
    }
}
