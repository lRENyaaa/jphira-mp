package top.rymc.phira.main.game.room.local;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRoomBuilderTest {

    @Test
    @DisplayName("should return correct default values for default settings")
    void shouldReturnCorrectDefaultValuesForDefaultSettings() {
        LocalRoomBuilder builder = new LocalRoomBuilder();

        LocalRoom.RoomSetting setting = builder.buildSetting();

        assertThat(setting.isAutoDestroy()).isTrue();
        assertThat(setting.isHost()).isTrue();
        assertThat(setting.getMaxPlayer()).isEqualTo(8);
        assertThat(setting.isLocked()).isFalse();
        assertThat(setting.isCycle()).isFalse();
        assertThat(setting.isLive()).isFalse();
        assertThat(setting.isChat()).isTrue();
    }

    @Test
    @DisplayName("should apply all values correctly for custom settings chain")
    void shouldApplyAllValuesCorrectlyForCustomSettingsChain() {
        LocalRoomBuilder builder = new LocalRoomBuilder()
            .autoDestroy(false)
            .host(false)
            .maxPlayer(16)
            .locked(true)
            .cycle(true)
            .live(true)
            .chat(false);

        LocalRoom.RoomSetting setting = builder.buildSetting();

        assertThat(setting.isAutoDestroy()).isFalse();
        assertThat(setting.isHost()).isFalse();
        assertThat(setting.getMaxPlayer()).isEqualTo(16);
        assertThat(setting.isLocked()).isTrue();
        assertThat(setting.isCycle()).isTrue();
        assertThat(setting.isLive()).isTrue();
        assertThat(setting.isChat()).isFalse();
    }

    @Test
    @DisplayName("should copy all values correctly from existing room setting")
    void shouldCopyAllValuesCorrectlyFromExistingRoomSetting() {
        LocalRoom.RoomSetting originalSetting = new LocalRoom.RoomSetting(
            false, true, 4, true, true, true, false
        );

        LocalRoomBuilder builder = new LocalRoomBuilder().setting(originalSetting);

        LocalRoom.RoomSetting copiedSetting = builder.buildSetting();

        assertThat(copiedSetting.isAutoDestroy()).isEqualTo(originalSetting.isAutoDestroy());
        assertThat(copiedSetting.isHost()).isEqualTo(originalSetting.isHost());
        assertThat(copiedSetting.getMaxPlayer()).isEqualTo(originalSetting.getMaxPlayer());
        assertThat(copiedSetting.isLocked()).isEqualTo(originalSetting.isLocked());
        assertThat(copiedSetting.isCycle()).isEqualTo(originalSetting.isCycle());
        assertThat(copiedSetting.isLive()).isEqualTo(originalSetting.isLive());
        assertThat(copiedSetting.isChat()).isEqualTo(originalSetting.isChat());
    }
}
