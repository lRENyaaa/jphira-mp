package top.rymc.phira.main.redis;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * mp:events 频道消息格式：{ "event": "EVENT_TYPE", "room_id": "1001", "data": { ... } }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PubSubEvent {
    private String event;
    @SerializedName("room_id")
    private String roomId;
    private Object data;
}
