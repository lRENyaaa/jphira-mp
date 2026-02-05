package top.rymc.phira.main.redis;

import top.rymc.phira.main.game.state.RoomGameState;
import top.rymc.phira.main.game.state.RoomPlaying;
import top.rymc.phira.main.game.state.RoomSelectChart;
import top.rymc.phira.main.game.state.RoomWaitForReady;

/**
 * 与 redis-data-schema 一致：0=SelectChart, 1=WaitingForReady, 2=Playing。
 */
public final class RoomStateCode {
    public static final int SELECT_CHART = 0;
    public static final int WAITING_FOR_READY = 1;
    public static final int PLAYING = 2;

    public static int from(RoomGameState state) {
        if (state instanceof RoomSelectChart) return SELECT_CHART;
        if (state instanceof RoomWaitForReady) return WAITING_FOR_READY;
        if (state instanceof RoomPlaying) return PLAYING;
        return SELECT_CHART;
    }

    private RoomStateCode() {}
}
