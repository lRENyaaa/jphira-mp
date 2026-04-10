package top.rymc.phira.main.game.exception.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.game.player.Player;

@RequiredArgsConstructor
@Getter
public class PlayerTypeMismatchException extends ResumeFailedException {

    private final Player player;

}
