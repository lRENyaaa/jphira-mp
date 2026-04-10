package top.rymc.phira.main.game.room.local;

import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.main.game.room.state.RoomGameState;

public class LocalRoomBuilder {

    private boolean autoDestroy = true;
    private boolean host = true;
    private int maxPlayer = 8;
    private boolean locked = false;
    private boolean cycle = false;
    private boolean live = false;
    private boolean chat = true;
    private RoomGameState.Type state = RoomGameState.Type.SelectChart;
    private ChartInfo chart;

    public LocalRoomBuilder autoDestroy(boolean autoDestroy) {
        this.autoDestroy = autoDestroy;
        return this;
    }

    public LocalRoomBuilder host(boolean host) {
        this.host = host;
        return this;
    }

    public LocalRoomBuilder maxPlayer(int maxPlayer) {
        this.maxPlayer = maxPlayer;
        return this;
    }

    public LocalRoomBuilder locked(boolean locked) {
        this.locked = locked;
        return this;
    }

    public LocalRoomBuilder cycle(boolean cycle) {
        this.cycle = cycle;
        return this;
    }

    public LocalRoomBuilder live(boolean live) {
        this.live = live;
        return this;
    }

    public LocalRoomBuilder chat(boolean chat) {
        this.chat = chat;
        return this;
    }

    public LocalRoomBuilder state(RoomGameState.Type state) {
        this.state = state;
        return this;
    }

    public LocalRoomBuilder chart(ChartInfo chart) {
        this.chart = chart;
        return this;
    }

    public LocalRoomBuilder setting(LocalRoom.RoomSetting setting) {
        this.autoDestroy = setting.isAutoDestroy();
        this.host = setting.isHost();
        this.maxPlayer = setting.getMaxPlayer();
        this.locked = setting.isLocked();
        this.cycle = setting.isCycle();
        this.live = setting.isLive();
        this.chat = setting.isChat();
        return this;
    }

    public LocalRoom.RoomSetting buildSetting() {
        return new LocalRoom.RoomSetting(
            autoDestroy, host, maxPlayer, locked, cycle, live, chat
        );
    }

    public LocalRoom build(String roomId) {
        return RoomManager.resolveRoom(roomId, (onDestroy) ->
                new LocalRoom(onDestroy, roomId, buildSetting(), state, chart)
        );
    }
}
