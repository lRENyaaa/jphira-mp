package top.rymc.phira.main.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import top.rymc.phira.protocol.data.UserProfile;

import java.time.OffsetDateTime;

@Getter
@ToString
@EqualsAndHashCode
@SuppressWarnings("unused")
public class UserInfo {

    private int id;
    private String name;

    public UserInfo() {}

    /** 仅用于 Redis 远端玩家占位（仅 id/name 有效）。 */
    public UserInfo(int id, String name) {
        this.id = id;
        this.name = name != null ? name : "";
    }
    private String avatar;
    private String language;
    private String bio;
    private int exp;
    private double rks;
    private OffsetDateTime joined;
    private OffsetDateTime lastLogin;
    private int roles;
    private boolean banned;
    private boolean loginBanned;
    private int followerCount;
    private int followingCount;
    private String email;

}