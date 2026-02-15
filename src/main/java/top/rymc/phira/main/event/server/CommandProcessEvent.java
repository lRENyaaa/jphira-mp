package top.rymc.phira.main.event.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.plugin.event.CancellableEvent;

@RequiredArgsConstructor
@Getter
public class CommandProcessEvent extends CancellableEvent {

    private final String command;

}
