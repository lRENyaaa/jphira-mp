package top.rymc.phira.main.http;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 服务器自签 JWT。所有 HTTP API 鉴权仅接受该 JWT，不使用 Phira 下发的 token。
 *
 * 密钥持久化于 data/jwt-secret（HS256，32 字节，首次自动生成）。
 */
public final class JwtService {

    private static final Path SECRET_FILE = Path.of("data", "jwt-secret");
    private static final long EXPIRATION_MILLIS = TimeUnit.HOURS.toMillis(24);
    private static final SecretKey KEY = loadKey();

    private JwtService() {
    }

    public static String issueToken(int userId, boolean isAdmin) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("isAdmin", isAdmin)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MILLIS))
                .signWith(KEY)
                .compact();
    }

    /**
     * 验证 JWT 并返回 userId；无效或过期返回 null。
     */
    public static Integer parseUserId(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token);
            return Integer.parseInt(jws.getPayload().getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey loadKey() {
        try {
            Files.createDirectories(SECRET_FILE.getParent());
            byte[] keyBytes;
            if (Files.exists(SECRET_FILE)) {
                keyBytes = Base64.getDecoder().decode(Files.readString(SECRET_FILE).trim());
            } else {
                keyBytes = new byte[32];
                new SecureRandom().nextBytes(keyBytes);
                Files.writeString(SECRET_FILE, Base64.getEncoder().encodeToString(keyBytes));
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load JWT secret", e);
        }
    }
}
