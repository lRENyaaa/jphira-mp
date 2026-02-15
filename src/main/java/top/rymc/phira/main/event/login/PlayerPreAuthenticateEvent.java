package top.rymc.phira.main.event.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.plugin.event.ReasonedCancellableEvent;

@Getter
@RequiredArgsConstructor
public class PlayerPreAuthenticateEvent extends ReasonedCancellableEvent {
    private final PlayerConnection connection;
    private final String token;

    @Setter
    private UserInfo userInfo;

}
