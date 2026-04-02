package top.rymc.phira.main.game.player;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.network.ProtocolConvertible;
// TODO import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.UserProfile;

import java.util.Optional;

public interface Player extends ProtocolConvertible<UserProfile> {

    Optional<Room> getRoom();

    /* // TODO using player interface in room
    default Optional<RoomInfo> getRoomInfo() {
        return getRoom().map(r -> r.asProtocolConvertible(this).toProtocol());
    }
     */

    void kick();

    UserInfo getUserInfo();

    default int getId() { return getUserInfo().getId(); }
    default String getName() { return getUserInfo().getName(); }

    default String getLanguage() {
        String lang = getUserInfo().getLanguage();
        return lang != null ? lang : Server.getInstance().getArgs().getDefaultLanguage();
    }
}
