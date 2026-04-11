package top.rymc.phira.plugin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.plugin.Listener;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.plugin.event.Event;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PluginEventBusTest {

    private PluginEventBus eventBus;
    private PluginContainer plugin;

    @BeforeEach
    void setUp() {
        eventBus = new PluginEventBus();
        plugin = mock(PluginContainer.class);
    }

    @Test
    @DisplayName("subscribe registers listener and receives events")
    void subscribeRegistersListenerAndReceivesEvents() {
        AtomicBoolean received = new AtomicBoolean(false);
        Listener listener = new TestListener() {
            @meteordevelopment.orbit.EventHandler
            void onTestEvent(TestEvent event) {
                received.set(true);
            }
        };

        eventBus.subscribe(plugin, listener);
        eventBus.post(new TestEvent());

        assertThat(received).isTrue();
    }

    @Test
    @DisplayName("post delivers event to subscribed listener")
    void postDeliversEventToSubscribedListener() {
        AtomicInteger value = new AtomicInteger(0);
        Listener listener = new TestListener() {
            @meteordevelopment.orbit.EventHandler
            void onTestEvent(TestEvent event) {
                value.set(event.getValue());
            }
        };

        eventBus.subscribe(plugin, listener);
        TestEvent event = new TestEvent();
        event.setValue(42);
        eventBus.post(event);

        assertThat(value.get()).isEqualTo(42);
    }

    @Test
    @DisplayName("post returns event after delivery")
    void postReturnsEventAfterDelivery() {
        Listener listener = new TestListener() {
            @meteordevelopment.orbit.EventHandler
            void onTestEvent(TestEvent event) {
            }
        };

        eventBus.subscribe(plugin, listener);
        TestEvent original = new TestEvent();
        TestEvent returned = eventBus.post(original);

        assertThat(returned).isSameAs(original);
    }

    @Test
    @DisplayName("unsubscribe removes listener and stops event delivery")
    void unsubscribeRemovesListenerAndStopsEventDelivery() {
        AtomicInteger count = new AtomicInteger(0);
        Listener listener = new TestListener() {
            @meteordevelopment.orbit.EventHandler
            void onTestEvent(TestEvent event) {
                count.incrementAndGet();
            }
        };

        eventBus.subscribe(plugin, listener);
        eventBus.post(new TestEvent());
        assertThat(count.get()).isEqualTo(1);

        eventBus.unsubscribe(plugin, listener);
        eventBus.post(new TestEvent());
        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("multiple listeners can subscribe to same event type")
    void multipleListenersCanSubscribeToSameEventType() {
        AtomicInteger count = new AtomicInteger(0);
        PluginContainer plugin2 = mock(PluginContainer.class);

        Listener listener1 = new TestListener() {
            @meteordevelopment.orbit.EventHandler
            void onTestEvent(TestEvent event) {
                count.incrementAndGet();
            }
        };
        Listener listener2 = new TestListener() {
            @meteordevelopment.orbit.EventHandler
            void onTestEvent(TestEvent event) {
                count.incrementAndGet();
            }
        };

        eventBus.subscribe(plugin, listener1);
        eventBus.subscribe(plugin2, listener2);
        eventBus.post(new TestEvent());

        assertThat(count.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("post cancellable event returns event with cancellation state")
    void postCancellableEventReturnsEventWithCancellationState() {
        Listener listener = new TestListener() {
            @meteordevelopment.orbit.EventHandler
            void onCancellableTestEvent(CancellableTestEvent event) {
                event.setCancelled(true);
            }
        };

        eventBus.subscribe(plugin, listener);
        CancellableTestEvent event = new CancellableTestEvent();
        CancellableTestEvent returned = eventBus.post(event);

        assertThat(returned.isCancelled()).isTrue();
    }

    private static class TestEvent extends Event {
        private int value;

        int getValue() {
            return value;
        }

        void setValue(int value) {
            this.value = value;
        }
    }

    private static class AnotherTestEvent extends Event {
    }

    private static class CancellableTestEvent extends CancellableEvent {
    }

    private static abstract class TestListener implements Listener {
        @Override
        public java.lang.invoke.MethodHandles.Lookup lookup() {
            return java.lang.invoke.MethodHandles.lookup();
        }
    }
}
