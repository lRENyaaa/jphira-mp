package top.rymc.phira.main.game.exception.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rymc.phira.main.game.player.Player;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SessionExceptionTest {

    @Mock
    private Player player;

    @Test
    @DisplayName("SuspendFailedException can be created")
    void suspendFailedExceptionCanBeCreated() {
        SuspendFailedException exception = new SuspendFailedException();

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("ResumeFailedException can be created")
    void resumeFailedExceptionCanBeCreated() {
        ResumeFailedException exception = new ResumeFailedException();

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PlayerTypeMismatchException stores player")
    void playerTypeMismatchExceptionStoresPlayer() {
        PlayerTypeMismatchException exception = new PlayerTypeMismatchException(player);

        assertThat(exception).isInstanceOf(ResumeFailedException.class);
        assertThat(exception.getPlayer()).isEqualTo(player);
    }
}
