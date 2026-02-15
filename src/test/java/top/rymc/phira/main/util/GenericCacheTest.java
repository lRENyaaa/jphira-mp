package top.rymc.phira.main.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("GenericCache")
class GenericCacheTest {

    private GenericCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = GenericCache.create(1, TimeUnit.HOURS, 100);
    }

    @Test
    @DisplayName("should return cached value")
    void shouldReturnCachedValue() {
        var value = cache.get("key", k -> "value");

        assertThat(value).isEqualTo("value");
    }

    @Test
    @DisplayName("should return same value for same key")
    void shouldReturnSameValueForSameKey() {
        var first = cache.get("key", k -> "first");
        var second = cache.get("key", k -> "second");

        assertThat(first).isEqualTo("first");
        assertThat(second).isEqualTo("first");
    }

    @Test
    @DisplayName("should call loader only once for same key")
    void shouldCallLoaderOnlyOnceForSameKey() {
        var counter = new AtomicInteger(0);

        cache.get("key", k -> {
            counter.incrementAndGet();
            return "value";
        });
        cache.get("key", k -> {
            counter.incrementAndGet();
            return "value";
        });

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should return null when loader returns null")
    void shouldReturnNullWhenLoaderReturnsNull() {
        var value = cache.get("key", k -> null);

        assertThat(value).isNull();
    }

    @Test
    @DisplayName("should expire entry after duration")
    void shouldExpireEntryAfterDuration() {
        var shortCache = GenericCache.create(100, TimeUnit.MILLISECONDS, 100);
        var counter = new AtomicInteger(0);

        shortCache.get("key", k -> {
            counter.incrementAndGet();
            return "value";
        });

        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                shortCache.get("key", k -> {
                    counter.incrementAndGet();
                    return "value";
                });
                assertThat(counter.get()).isEqualTo(2);
            });
    }

    @Test
    @DisplayName("should invalidate specific key")
    void shouldInvalidateSpecificKey() {
        cache.get("key1", k -> "value1");
        cache.get("key2", k -> "value2");

        cache.invalidate("key1");

        var counter = new AtomicInteger(0);
        cache.get("key1", k -> {
            counter.incrementAndGet();
            return "new-value1";
        });
        cache.get("key2", k -> {
            counter.incrementAndGet();
            return "new-value2";
        });

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should invalidate multiple keys")
    void shouldInvalidateMultipleKeys() {
        cache.get("key1", k -> "value1");
        cache.get("key2", k -> "value2");
        cache.get("key3", k -> "value3");

        cache.invalidateAll(java.util.List.of("key1", "key2"));

        var counter = new AtomicInteger(0);
        cache.get("key1", k -> { counter.incrementAndGet(); return "v"; });
        cache.get("key2", k -> { counter.incrementAndGet(); return "v"; });
        cache.get("key3", k -> { counter.incrementAndGet(); return "v"; });

        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("should clear all entries")
    void shouldClearAllEntries() {
        cache.get("key1", k -> "value1");
        cache.get("key2", k -> "value2");

        cache.clear();

        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("should return estimated size")
    void shouldReturnEstimatedSize() {
        cache.get("key1", k -> "value1");
        cache.get("key2", k -> "value2");

        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("should return stats")
    void shouldReturnStats() {
        cache.get("key1", k -> "value1");
        cache.get("key1", k -> "value1");

        var stats = cache.stats();

        assertThat(stats).contains("hitCount");
        assertThat(stats).contains("missCount");
    }

    @Test
    @DisplayName("should handle different key types")
    void shouldHandleDifferentKeyTypes() {
        GenericCache<Integer, String> intKeyCache = GenericCache.create(1, TimeUnit.HOURS, 100);

        var value = intKeyCache.get(42, k -> "answer");

        assertThat(value).isEqualTo("answer");
    }

    @Test
    @DisplayName("should handle exception from loader")
    void shouldHandleExceptionFromLoader() {
        class TestException extends Exception {}

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            cache.get("key", k -> { throw new TestException(); })
        ).isInstanceOf(TestException.class);
    }
}
