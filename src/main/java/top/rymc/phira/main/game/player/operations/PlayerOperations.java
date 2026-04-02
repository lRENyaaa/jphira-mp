package top.rymc.phira.main.game.player.operations;

import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;

import java.util.List;

public interface PlayerOperations {

    void updateHostStatus(boolean isHost);

    void enterState(GameState state);

    void selectChart(int chartId, String chartName, int selectedBy);

    void lockRoom(boolean locked);

    void cycleRoom(boolean cycle);

    void memberJoined(int memberId, String memberName, boolean isMonitor);

    void memberLeft(int memberId, String memberName);

    void memberReady(int memberId);

    void memberCancelReady(int memberId);

    void gameRequireStart(int initiatorId);

    void gameStartPlaying();

    void gameAbort(int abortedBy);

    void receiveTouchStream(int fromPlayerId, List<TouchFrame> frames);

    void receiveJudgeStream(int fromPlayerId, List<JudgeEvent> events);

    void receiveChat(int senderId, String message);

    void gameEnd();

    void gamePlayed(int id, int score, float accuracy, boolean fullCombo);

}
