package top.rymc.phira.main.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class PlayerPostJoinEvent extends ReasonedCancellableEvent {

    private final Player player;

}
