package top.rymc.phira.main.game.player.operations;

import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.message.*;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeHostPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeStatePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJudgesPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundOnJoinRoomPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundTouchesPacket;

import java.util.List;

public class LocalPlayerOperations implements PlayerOperations {
    private final PlayerConnection connection;

    public LocalPlayerOperations(PlayerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void updateHostStatus(boolean isHost) {
        connection.send(ClientBoundChangeHostPacket.create(isHost));
    }

    @Override
    public void enterState(GameState state) {
        connection.send(ClientBoundChangeStatePacket.create(state));
    }

    @Override
    public void selectChart(int chartId, String chartName, int selectedBy) {
        connection.send(ClientBoundMessagePacket.create(
            new SelectChartMessage(selectedBy, chartName, chartId)
        ));
        connection.send(ClientBoundChangeStatePacket.create(new SelectChart(chartId)));
    }

    @Override
    public void lockRoom(boolean locked) {
        connection.send(ClientBoundMessagePacket.create(new LockRoomMessage(locked)));
    }

    @Override
    public void cycleRoom(boolean cycle) {
        connection.send(ClientBoundMessagePacket.create(new CycleRoomMessage(cycle)));
    }

    @Override
    public void memberJoined(int memberId, String memberName, boolean isMonitor) {
        connection.send(ClientBoundOnJoinRoomPacket.create(new FullUserProfile(memberId, memberName, isMonitor)));
        connection.send(ClientBoundMessagePacket.create(
            new JoinRoomMessage(memberId, memberName)
        ));
    }

    @Override
    public void memberLeft(int memberId, String memberName) {
        connection.send(ClientBoundMessagePacket.create(
            new LeaveRoomMessage(memberId, memberName)
        ));
    }

    @Override
    public void memberReady(int memberId) {
        connection.send(ClientBoundMessagePacket.create(new ReadyMessage(memberId)));
    }

    @Override
    public void memberCancelReady(int memberId) {
        connection.send(ClientBoundMessagePacket.create(new CancelReadyMessage(memberId)));
    }

    @Override
    public void gameRequireStart(int initiatorId) {
        connection.send(ClientBoundMessagePacket.create(new GameStartMessage(initiatorId)));
    }

    @Override
    public void gameStartPlaying() {
        connection.send(ClientBoundMessagePacket.create(StartPlayingMessage.INSTANCE));
    }

    @Override
    public void gameAbort(int abortedBy) {
        connection.send(ClientBoundMessagePacket.create(new AbortMessage(abortedBy)));
    }

    @Override
    public void receiveTouchStream(int fromPlayerId, List<TouchFrame> frames) {
        connection.send(ClientBoundTouchesPacket.create(fromPlayerId, frames));
    }

    @Override
    public void receiveJudgeStream(int fromPlayerId, List<JudgeEvent> events) {
        connection.send(ClientBoundJudgesPacket.create(fromPlayerId, events));
    }

    @Override
    public void receiveChat(int senderId, String message) {
        connection.send(ClientBoundMessagePacket.create(new ChatMessage(senderId, message)));
    }

    public void gameEnd() {
        connection.send(ClientBoundMessagePacket.create(GameEndMessage.INSTANCE));
    }

    public void gamePlayed(int id, int score, float accuracy, boolean fullCombo) {
        connection.send(ClientBoundMessagePacket.create( new PlayedMessage(id, score, accuracy, fullCombo)));
    }

}
