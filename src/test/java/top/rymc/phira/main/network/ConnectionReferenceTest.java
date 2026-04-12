package top.rymc.phira.main.network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ConnectionReferenceTest {

    @Mock
    private PlayerConnection initialConnection;

    @Mock
    private PlayerConnection newConnection;

    private ConnectionReference connectionReference;

    @BeforeEach
    void setUp() {
        connectionReference = new ConnectionReference(initialConnection);
    }

    @Test
    @DisplayName("get returns current connection")
    void getReturnsCurrentConnection() {
        PlayerConnection result = connectionReference.get();

        assertThat(result).isEqualTo(initialConnection);
    }

    @Test
    @DisplayName("resume updates reference to new connection")
    void resumeUpdatesReferenceToNewConnection() {
        Consumer<PlayerConnection> onDuplicate = mock(Consumer.class);

        connectionReference.resume(newConnection, onDuplicate);

        assertThat(connectionReference.get()).isEqualTo(newConnection);
    }

    @Test
    @DisplayName("resume with existing connection marks old as duplicate and executes callback")
    void resumeWithExistingConnectionMarksOldAsDuplicateAndExecutesCallback() {
        Consumer<PlayerConnection> onDuplicate = mock(Consumer.class);

        connectionReference.resume(newConnection, onDuplicate);

        verify(onDuplicate).accept(initialConnection);
        verify(initialConnection).markDuplicateLogin();
        assertThat(connectionReference.get()).isEqualTo(newConnection);
    }

    @Test
    @DisplayName("should only update reference when resume with null existing connection")
    void shouldOnlyUpdateReferenceWhenResumeWithNullExistingConnection() {
        connectionReference = new ConnectionReference(null);
        Consumer<PlayerConnection> onDuplicate = mock(Consumer.class);

        connectionReference.resume(newConnection, onDuplicate);

        verify(onDuplicate, never()).accept(null);
        assertThat(connectionReference.get()).isEqualTo(newConnection);
    }
}
