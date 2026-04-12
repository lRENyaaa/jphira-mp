package top.rymc.phira.main.game.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameOperationExceptionTest {

    @Test
    @DisplayName("should create invalidState exception with correct message key")
    void shouldCreateInvalidStateExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.invalidState();

        assertThat(exception.getMessageKey()).isEqualTo("error.invalid_state");
    }

    @Test
    @DisplayName("should create permissionDenied exception with correct message key")
    void shouldCreatePermissionDeniedExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.permissionDenied();

        assertThat(exception.getMessageKey()).isEqualTo("error.permission_denied");
    }

    @Test
    @DisplayName("should create roomFull exception with correct message key")
    void shouldCreateRoomFullExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.roomFull();

        assertThat(exception.getMessageKey()).isEqualTo("error.room_full");
    }

    @Test
    @DisplayName("should create roomLocked exception with correct message key")
    void shouldCreateRoomLockedExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.roomLocked();

        assertThat(exception.getMessageKey()).isEqualTo("error.room_locked");
    }

    @Test
    @DisplayName("should create roomNotFound exception with correct message key")
    void shouldCreateRoomNotFoundExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.roomNotFound();

        assertThat(exception.getMessageKey()).isEqualTo("error.room_not_found");
    }

    @Test
    @DisplayName("should create roomAlreadyExists exception with correct message key")
    void shouldCreateRoomAlreadyExistsExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.roomAlreadyExists();

        assertThat(exception.getMessageKey()).isEqualTo("error.room_already_exists");
    }

    @Test
    @DisplayName("should create chartNotSelected exception with correct message key")
    void shouldCreateChartNotSelectedExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.chartNotSelected();

        assertThat(exception.getMessageKey()).isEqualTo("error.chart_not_selected");
    }

    @Test
    @DisplayName("should create chartNotFound exception with correct message key")
    void shouldCreateChartNotFoundExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.chartNotFound();

        assertThat(exception.getMessageKey()).isEqualTo("error.chart_not_found");
    }

    @Test
    @DisplayName("should create chatNotEnabled exception with correct message key")
    void shouldCreateChatNotEnabledExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.chatNotEnabled();

        assertThat(exception.getMessageKey()).isEqualTo("error.chat_not_enabled");
    }

    @Test
    @DisplayName("should create recordNotFound exception with correct message key")
    void shouldCreateRecordNotFoundExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.recordNotFound();

        assertThat(exception.getMessageKey()).isEqualTo("error.record_not_found");
    }

    @Test
    @DisplayName("should create alreadyInRoom exception with correct message key")
    void shouldCreateAlreadyInRoomExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.alreadyInRoom();

        assertThat(exception.getMessageKey()).isEqualTo("error.already_in_room");
    }

    @Test
    @DisplayName("should create notInRoom exception with correct message key")
    void shouldCreateNotInRoomExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.notInRoom();

        assertThat(exception.getMessageKey()).isEqualTo("error.not_in_room");
    }

    @Test
    @DisplayName("should create notHost exception with correct message key")
    void shouldCreateNotHostExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.notHost();

        assertThat(exception.getMessageKey()).isEqualTo("error.not_host");
    }

    @Test
    @DisplayName("should create playerNotFound exception with correct message key")
    void shouldCreatePlayerNotFoundExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.playerNotFound();

        assertThat(exception.getMessageKey()).isEqualTo("error.player_not_found");
    }

    @Test
    @DisplayName("should create sessionExpired exception with correct message key")
    void shouldCreateSessionExpiredExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.sessionExpired();

        assertThat(exception.getMessageKey()).isEqualTo("error.session_expired");
    }

    @Test
    @DisplayName("should create authenticationFailed exception with correct message key")
    void shouldCreateAuthenticationFailedExceptionWithCorrectMessageKey() {
        GameOperationException exception = GameOperationException.authenticationFailed();

        assertThat(exception.getMessageKey()).isEqualTo("error.authentication_failed");
    }
}
