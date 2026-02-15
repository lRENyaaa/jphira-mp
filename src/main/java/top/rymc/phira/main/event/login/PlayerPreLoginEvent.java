package top.rymc.phira.main.event.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@RequiredArgsConstructor
@Getter
public class PlayerPreLoginEvent extends ReasonedCancellableEvent {

    private final UserInfo userInfo;

}
