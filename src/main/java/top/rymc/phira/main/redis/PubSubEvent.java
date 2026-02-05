package top.rymc.phira.main.redis;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * mp:events 频道消息格式：{ "event": "EVENT_TYPE", "room_id": "1001", "server_id": "jphira-1", "data": { ... } }
 * server_id 用于订阅端忽略本机发出的事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PubSubEvent {
    private String event;
    @SerializedName("room_id")
    private String roomId;
    @SerializedName("server_id")
    private String serverId;
    private Object data;
}
