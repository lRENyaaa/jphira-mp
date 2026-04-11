package top.rymc.phira.main.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class GenericCacheTest {

    private GenericCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = GenericCache.create(1, TimeUnit.HOURS, 100);
    }

    @Test
    @DisplayName("get calls loader function when key not in cache")
    void getCallsLoaderWhenKeyNotInCache() {
        AtomicInteger callCount = new AtomicInteger(0);

        String result = cache.get("key1", k -> {
            callCount.incrementAndGet();
            return "value1";
        });

        assertThat(result).isEqualTo("value1");
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("get returns cached value without calling loader when key exists")
    void getReturnsCachedValueWithoutCallingLoaderWhenKeyExists() {
        AtomicInteger callCount = new AtomicInteger(0);

        cache.get("key1", k -> {
            callCount.incrementAndGet();
            return "value1";
        });

        String result = cache.get("key1", k -> {
            callCount.incrementAndGet();
            return "value2";
        });

        assertThat(result).isEqualTo("value1");
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("invalidate removes key from cache")
    void invalidateRemovesKeyFromCache() {
        AtomicInteger callCount = new AtomicInteger(0);

        cache.get("key1", k -> "value1");
        cache.invalidate("key1");

        String result = cache.get("key1", k -> {
            callCount.incrementAndGet();
            return "value2";
        });

        assertThat(result).isEqualTo("value2");
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("clear removes all keys from cache")
    void clearRemovesAllKeysFromCache() {
        AtomicInteger callCount = new AtomicInteger(0);

        cache.get("key1", k -> "value1");
        cache.get("key2", k -> "value2");
        cache.clear();

        String result1 = cache.get("key1", k -> {
            callCount.incrementAndGet();
            return "newValue1";
        });
        String result2 = cache.get("key2", k -> {
            callCount.incrementAndGet();
            return "newValue2";
        });

        assertThat(result1).isEqualTo("newValue1");
        assertThat(result2).isEqualTo("newValue2");
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("size returns estimated cache size")
    void sizeReturnsEstimatedCacheSize() {
        assertThat(cache.size()).isEqualTo(0);

        cache.get("key1", k -> "value1");
        assertThat(cache.size()).isEqualTo(1);

        cache.get("key2", k -> "value2");
        assertThat(cache.size()).isEqualTo(2);

        cache.invalidate("key1");
        assertThat(cache.size()).isEqualTo(1);

        cache.clear();
        assertThat(cache.size()).isEqualTo(0);
    }
}
