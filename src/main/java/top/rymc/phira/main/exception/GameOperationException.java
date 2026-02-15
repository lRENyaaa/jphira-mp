package top.rymc.phira.main.exception;

public class GameOperationException extends RuntimeException {

    public GameOperationException(String message) {
        super(message);
    }

    public static GameOperationException invalidState() {
        return new GameOperationException("你不能在当前状态执行这个操作");
    }

    public static GameOperationException permissionDenied() {
        return new GameOperationException("你没有权限");
    }

    public static GameOperationException roomFull() {
        return new GameOperationException("Room is full");
    }

    public static GameOperationException roomLocked() {
        return new GameOperationException("Room is locked");
    }

    public static GameOperationException roomNotFound() {
        return new GameOperationException("房间不存在");
    }

    public static GameOperationException roomAlreadyExists() {
        return new GameOperationException("Room already exists");
    }

    public static GameOperationException chartNotSelected() {
        return new GameOperationException("未选择谱面");
    }

    public static GameOperationException chartNotFound() {
        return new GameOperationException("谱面信息获取失败");
    }

    public static GameOperationException chatNotEnabled() {
        return new GameOperationException("房间未启用聊天");
    }

    public static GameOperationException recordNotFound() {
        return new GameOperationException("查询记录失败");
    }
}
