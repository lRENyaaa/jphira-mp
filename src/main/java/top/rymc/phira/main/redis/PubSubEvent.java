package top.rymc.phira.main.redis;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * 跨服务器事件 JSON 结构，对应 redis-data-schema.md 3.1
 */
public final class PubSubEvent {

    private String event;
    @SerializedName("room_id")
    private String roomId;
    private Map<String, Object> data;

    public PubSubEvent() {}

    public PubSubEvent(String event, String roomId, Map<String, Object> data) {
        this.event = event;
        this.roomId = roomId;
        this.data = data;
    }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    /** 事件类型常量 */
    public static final String PLAYER_JOIN = "PLAYER_JOIN";
    public static final String PLAYER_LEAVE = "PLAYER_LEAVE";
    public static final String STATE_CHANGE = "STATE_CHANGE";
    public static final String SYNC_SCORE = "SYNC_SCORE";
}
