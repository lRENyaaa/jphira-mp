package top.rymc.phira.main.util;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GsonUtilTest {

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = GsonUtil.getGson();
    }

    @Test
    @DisplayName("OffsetDateTime serialization produces ISO-8601 format")
    void offsetDateTimeSerializationProducesIso8601Format() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 3, 15, 10, 30, 45, 0, ZoneOffset.UTC);

        String json = gson.toJson(dateTime);

        assertThat(json).isEqualTo("\"2024-03-15T10:30:45Z\"");
    }

    @Test
    @DisplayName("OffsetDateTime serialization with non-UTC offset produces ISO-8601 format with offset")
    void offsetDateTimeSerializationWithNonUtcOffsetProducesIso8601FormatWithOffset() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 3, 15, 10, 30, 45, 0, ZoneOffset.ofHours(8));

        String json = gson.toJson(dateTime);

        assertThat(json).isEqualTo("\"2024-03-15T10:30:45+08:00\"");
    }

    @Test
    @DisplayName("OffsetDateTime deserialization from ISO-8601 string with Z suffix")
    void offsetDateTimeDeserializationFromIso8601StringWithZSuffix() {
        String json = "\"2024-03-15T10:30:45Z\"";

        OffsetDateTime result = gson.fromJson(json, OffsetDateTime.class);

        assertThat(result).isEqualTo(OffsetDateTime.of(2024, 3, 15, 10, 30, 45, 0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("OffsetDateTime deserialization from ISO-8601 string with offset")
    void offsetDateTimeDeserializationFromIso8601StringWithOffset() {
        String json = "\"2024-03-15T10:30:45+08:00\"";

        OffsetDateTime result = gson.fromJson(json, OffsetDateTime.class);

        assertThat(result).isEqualTo(OffsetDateTime.of(2024, 3, 15, 10, 30, 45, 0, ZoneOffset.ofHours(8)));
    }

    @Test
    @DisplayName("OffsetDateTime serialization and deserialization round trip")
    void offsetDateTimeSerializationAndDeserializationRoundTrip() {
        OffsetDateTime original = OffsetDateTime.of(2024, 6, 20, 15, 45, 30, 123456789, ZoneOffset.ofHoursMinutes(-5, -30));

        String json = gson.toJson(original);
        OffsetDateTime result = gson.fromJson(json, OffsetDateTime.class);

        assertThat(result).isEqualTo(original);
    }

    @Test
    @DisplayName("Field naming policy converts camelCase to underscore_names in serialization")
    void fieldNamingPolicyConvertsCamelCaseToUnderscoreNamesInSerialization() {
        TestDataClass data = new TestDataClass();
        data.setUserName("testUser");
        data.setEmailAddress("test@example.com");
        data.setPhoneNumber("1234567890");

        String json = gson.toJson(data);

        assertThat(json).contains("\"user_name\": \"testUser\"");
        assertThat(json).contains("\"email_address\": \"test@example.com\"");
        assertThat(json).contains("\"phone_number\": \"1234567890\"");
    }

    @Test
    @DisplayName("Field naming policy converts underscore_names to camelCase in deserialization")
    void fieldNamingPolicyConvertsUnderscoreNamesToCamelCaseInDeserialization() {
        String json = "{\"user_name\":\"testUser\",\"email_address\":\"test@example.com\",\"phone_number\":\"1234567890\"}";

        TestDataClass result = gson.fromJson(json, TestDataClass.class);

        assertThat(result.getUserName()).isEqualTo("testUser");
        assertThat(result.getEmailAddress()).isEqualTo("test@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("Field naming policy handles nested objects with camelCase conversion")
    void fieldNamingPolicyHandlesNestedObjectsWithCamelCaseConversion() {
        NestedTestDataClass data = new NestedTestDataClass();
        data.setOuterField("outerValue");
        TestDataClass inner = new TestDataClass();
        inner.setUserName("innerUser");
        inner.setEmailAddress("inner@example.com");
        data.setInnerObject(inner);

        String json = gson.toJson(data);

        assertThat(json).contains("\"outer_field\": \"outerValue\"");
        assertThat(json).contains("\"inner_object\"");
        assertThat(json).contains("\"user_name\": \"innerUser\"");
    }

    @Test
    @DisplayName("Field naming policy handles single word fields without conversion")
    void fieldNamingPolicyHandlesSingleWordFieldsWithoutConversion() {
        SingleWordFieldClass data = new SingleWordFieldClass();
        data.setName("testName");
        data.setId(123);

        String json = gson.toJson(data);

        assertThat(json).contains("\"name\": \"testName\"");
        assertThat(json).contains("\"id\": 123");
    }

    @Test
    @DisplayName("Field naming policy handles consecutive uppercase letters")
    void fieldNamingPolicyHandlesConsecutiveUppercaseLetters() {
        ConsecutiveUppercaseClass data = new ConsecutiveUppercaseClass();
        data.setUserId("user123");
        data.setHtmlEntity("test");
        data.setXmlData("data");

        String json = gson.toJson(data);

        assertThat(json).contains("\"user_id\": \"user123\"");
        assertThat(json).contains("\"html_entity\": \"test\"");
        assertThat(json).contains("\"xml_data\": \"data\"");
    }

    @Test
    @DisplayName("Field naming policy round trip serialization and deserialization")
    void fieldNamingPolicyRoundTripSerializationAndDeserialization() {
        TestDataClass original = new TestDataClass();
        original.setUserName("roundTripUser");
        original.setEmailAddress("roundtrip@example.com");
        original.setPhoneNumber("9876543210");

        String json = gson.toJson(original);
        TestDataClass result = gson.fromJson(json, TestDataClass.class);

        assertThat(result.getUserName()).isEqualTo(original.getUserName());
        assertThat(result.getEmailAddress()).isEqualTo(original.getEmailAddress());
        assertThat(result.getPhoneNumber()).isEqualTo(original.getPhoneNumber());
    }

    @Test
    @DisplayName("Gson instance is singleton and returns same instance")
    void gsonInstanceIsSingletonAndReturnsSameInstance() {
        Gson first = GsonUtil.getGson();
        Gson second = GsonUtil.getGson();

        assertThat(first).isSameAs(second);
    }

    static class TestDataClass {
        private String userName;
        private String emailAddress;
        private String phoneNumber;

        String getUserName() {
            return userName;
        }

        void setUserName(String userName) {
            this.userName = userName;
        }

        String getEmailAddress() {
            return emailAddress;
        }

        void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

        String getPhoneNumber() {
            return phoneNumber;
        }

        void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    static class NestedTestDataClass {
        private String outerField;
        private TestDataClass innerObject;

        String getOuterField() {
            return outerField;
        }

        void setOuterField(String outerField) {
            this.outerField = outerField;
        }

        TestDataClass getInnerObject() {
            return innerObject;
        }

        void setInnerObject(TestDataClass innerObject) {
            this.innerObject = innerObject;
        }
    }

    static class SingleWordFieldClass {
        private String name;
        private int id;

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        int getId() {
            return id;
        }

        void setId(int id) {
            this.id = id;
        }
    }

    static class ConsecutiveUppercaseClass {
        private String userId;
        private String htmlEntity;
        private String xmlData;

        String getUserId() {
            return userId;
        }

        void setUserId(String userId) {
            this.userId = userId;
        }

        String getHtmlEntity() {
            return htmlEntity;
        }

        void setHtmlEntity(String htmlEntity) {
            this.htmlEntity = htmlEntity;
        }

        String getXmlData() {
            return xmlData;
        }

        void setXmlData(String xmlData) {
            this.xmlData = xmlData;
        }
    }
}
