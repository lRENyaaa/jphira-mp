package top.rymc.phira.plugin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rymc.phira.plugin.Listener;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.plugin.event.Event;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PluginEventBusTest {

    private PluginEventBus eventBus;
    private PluginContainer plugin;

    @BeforeEach
    void setUp() {
        eventBus = new PluginEventBus();
        plugin = mock(PluginContainer.class);
    }

    @Test
    @DisplayName("should register listener and receive events when subscribe")
    void shouldRegisterListenerAndReceiveEventsWhenSubscribe() {
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
    @DisplayName("should deliver event to subscribed listener when post")
    void shouldDeliverEventToSubscribedListenerWhenPost() {
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
    @DisplayName("should return event after delivery when post")
    void shouldReturnEventAfterDeliveryWhenPost() {
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
    @DisplayName("should remove listener and stop event delivery when unsubscribe")
    void shouldRemoveListenerAndStopEventDeliveryWhenUnsubscribe() {
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
    @DisplayName("should allow multiple listeners to subscribe to same event type")
    void shouldAllowMultipleListenersToSubscribeToSameEventType() {
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
    @DisplayName("should return event with cancellation state when post cancellable event")
    void shouldReturnEventWithCancellationStateWhenPostCancellableEvent() {
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
