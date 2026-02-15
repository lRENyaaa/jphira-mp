package top.rymc.phira.main.event.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class PlayerPostLoginEvent extends ReasonedCancellableEvent {

    private final Player player;

}
