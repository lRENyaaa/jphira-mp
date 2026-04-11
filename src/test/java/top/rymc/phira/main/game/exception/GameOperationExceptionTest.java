package top.rymc.phira.main.game.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameOperationExceptionTest {

    @Test
    @DisplayName("invalidState factory creates exception with correct message key")
    void invalidStateFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.invalidState();

        assertThat(exception.getMessageKey()).isEqualTo("error.invalid_state");
    }

    @Test
    @DisplayName("permissionDenied factory creates exception with correct message key")
    void permissionDeniedFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.permissionDenied();

        assertThat(exception.getMessageKey()).isEqualTo("error.permission_denied");
    }

    @Test
    @DisplayName("roomFull factory creates exception with correct message key")
    void roomFullFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.roomFull();

        assertThat(exception.getMessageKey()).isEqualTo("error.room_full");
    }

    @Test
    @DisplayName("roomLocked factory creates exception with correct message key")
    void roomLockedFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.roomLocked();

        assertThat(exception.getMessageKey()).isEqualTo("error.room_locked");
    }

    @Test
    @DisplayName("roomNotFound factory creates exception with correct message key")
    void roomNotFoundFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.roomNotFound();

        assertThat(exception.getMessageKey()).isEqualTo("error.room_not_found");
    }

    @Test
    @DisplayName("roomAlreadyExists factory creates exception with correct message key")
    void roomAlreadyExistsFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.roomAlreadyExists();

        assertThat(exception.getMessageKey()).isEqualTo("error.room_already_exists");
    }

    @Test
    @DisplayName("chartNotSelected factory creates exception with correct message key")
    void chartNotSelectedFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.chartNotSelected();

        assertThat(exception.getMessageKey()).isEqualTo("error.chart_not_selected");
    }

    @Test
    @DisplayName("chartNotFound factory creates exception with correct message key")
    void chartNotFoundFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.chartNotFound();

        assertThat(exception.getMessageKey()).isEqualTo("error.chart_not_found");
    }

    @Test
    @DisplayName("chatNotEnabled factory creates exception with correct message key")
    void chatNotEnabledFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.chatNotEnabled();

        assertThat(exception.getMessageKey()).isEqualTo("error.chat_not_enabled");
    }

    @Test
    @DisplayName("recordNotFound factory creates exception with correct message key")
    void recordNotFoundFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.recordNotFound();

        assertThat(exception.getMessageKey()).isEqualTo("error.record_not_found");
    }

    @Test
    @DisplayName("alreadyInRoom factory creates exception with correct message key")
    void alreadyInRoomFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.alreadyInRoom();

        assertThat(exception.getMessageKey()).isEqualTo("error.already_in_room");
    }

    @Test
    @DisplayName("notInRoom factory creates exception with correct message key")
    void notInRoomFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.notInRoom();

        assertThat(exception.getMessageKey()).isEqualTo("error.not_in_room");
    }

    @Test
    @DisplayName("notHost factory creates exception with correct message key")
    void notHostFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.notHost();

        assertThat(exception.getMessageKey()).isEqualTo("error.not_host");
    }

    @Test
    @DisplayName("playerNotFound factory creates exception with correct message key")
    void playerNotFoundFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.playerNotFound();

        assertThat(exception.getMessageKey()).isEqualTo("error.player_not_found");
    }

    @Test
    @DisplayName("sessionExpired factory creates exception with correct message key")
    void sessionExpiredFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.sessionExpired();

        assertThat(exception.getMessageKey()).isEqualTo("error.session_expired");
    }

    @Test
    @DisplayName("authenticationFailed factory creates exception with correct message key")
    void authenticationFailedFactoryCreatesExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.authenticationFailed();

        assertThat(exception.getMessageKey()).isEqualTo("error.authentication_failed");
    }
}
