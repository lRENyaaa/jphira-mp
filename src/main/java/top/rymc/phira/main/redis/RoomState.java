package top.rymc.phira.main.redis;

/**
 * 房间状态枚举，与规范一致：0 SelectChart, 1 WaitingForReady, 2 Playing
 */
public enum RoomState {
    SelectChart(0),
    WaitingForReady(1),
    Playing(2);

    private final int code;

    RoomState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RoomState fromCode(int code) {
        for (RoomState s : values()) {
            if (s.code == code) return s;
        }
        return SelectChart;
    }
}
