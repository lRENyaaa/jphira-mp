package top.rymc.phira.main.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.json.JavalinGson;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.PlayerManager;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.main.game.room.RoomSnapshot;
import top.rymc.phira.main.game.room.chart.ChartPool;
import top.rymc.phira.main.game.room.chart.RoomChartPool;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.main.game.room.local.LocalRoomBuilder;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.game.room.state.RoomPlaying;
import top.rymc.phira.main.game.room.state.RoomWaitForReady;
import top.rymc.phira.main.util.GsonUtil;
import top.rymc.phira.main.util.PhiraFetcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import static java.util.Map.entry;

/**
 * HTTP 控制面 API 服务器（Javalin）。
 *
 * - 静态前端资源挂载 ./frontend/dist（Location.EXTERNAL）
 * - API 路径遵循 /api/v1/... 文档
 * - 除 /api/v1/login 外均需 Bearer Phira Token；写操作需管理员权限（data/admins.json）
 */
public final class ApiServer {

    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,20}");
    private static final CountDownLatch STARTED = new CountDownLatch(1);

    private ApiServer() {
    }

    public static void start(String host, int port) {
        Thread thread = new Thread(() -> {
            try {
                Javalin.create(ApiServer::configure).start(host, port);
                Server.getLogger().info("HTTP API server listening on {}:{}", host, port);
            } catch (Exception e) {
                Server.getLogger().error("Failed to start HTTP API server", e);
            } finally {
                STARTED.countDown();
            }
        }, "Http-ApiServer");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 阻塞直到 HTTP API 服务器启动完成（或启动失败），确保服务端 "Done" 日志在最后输出。
     */
    public static void awaitStarted() {
        try {
            STARTED.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void configure(io.javalin.config.JavalinConfig config) {
        config.jsonMapper(new JavalinGson(GsonUtil.getCompactGson(), false));

        config.routes.before("/api/v1/*", ApiServer::authenticate);
        config.routes.after("/api/v1/*", ApiServer::corsHeaders);
        config.routes.options("/api/v1/*", ctx -> ctx.status(204));

        config.routes.post("/api/v1/login", ApiServer::handleLogin);

        config.routes.post("/api/v1/room/{id}/create", ApiServer::handleRoomCreate);
        config.routes.put("/api/v1/room/{id}/update", ApiServer::handleRoomUpdate);
        // 具体路径必须先于参数路径注册（Javalin 按注册顺序匹配，避免 /room/list 被 /room/{id} 捕获）
        config.routes.get("/api/v1/room/list", ApiServer::handleRoomList);
        config.routes.get("/api/v1/room/{id}/", ApiServer::handleRoomGet);
        config.routes.get("/api/v1/room/{id}", ApiServer::handleRoomGet);
        config.routes.delete("/api/v1/room/{id}", ApiServer::handleRoomDelete);
        config.routes.post("/api/v1/room/{id}/end", ApiServer::handleRoomEnd);
        config.routes.get("/api/v1/room/{id}/pool", ApiServer::handleRoomPoolGet);
        config.routes.put("/api/v1/room/{id}/pool/switch", ApiServer::handleRoomPoolSwitch);
        config.routes.put("/api/v1/room/{id}/pool/favorite", ApiServer::handleRoomPoolFavorite);

        config.routes.get("/api/v1/pool/list", ApiServer::handlePoolList);
        config.routes.post("/api/v1/pool", ApiServer::handlePoolCreate);
        config.routes.delete("/api/v1/pool/{id}", ApiServer::handlePoolRemove);
        config.routes.post("/api/v1/pool/{id}/chart", ApiServer::handlePoolChartAdd);
        config.routes.delete("/api/v1/pool/{id}/chart/{chartId}", ApiServer::handlePoolChartRemove);
        config.routes.put("/api/v1/pool/{id}/favorite", ApiServer::handlePoolFavorite);
        config.routes.put("/api/v1/pool/{id}/default", ApiServer::handlePoolDefault);

        // 前端静态资源与 SPA 路由 fallback（API 具体路由优先匹配，通配在此兜底）
        config.routes.get("/", ApiServer::serveFrontend);
        config.routes.get("/*", ApiServer::serveFrontend);

        config.routes.exception(ApiException.class, (e, ctx) ->
                ctx.status(e.status).json(Map.of("ok", false, "reason", e.reason)));
        config.routes.exception(Exception.class, (e, ctx) -> {
            Server.getLogger().error("HTTP API error on {}", ctx.path(), e);
            ctx.status(500).json(Map.of("ok", false, "reason", "服务器内部错误"));
        });
    }

    // ===== 前端静态资源与 SPA fallback =====

    private static final Path FRONTEND_DIR = Path.of("frontend", "dist").toAbsolutePath().normalize();

    private static final Map<String, String> STATIC_MIME = Map.ofEntries(
            entry("html", "text/html; charset=utf-8"),
            entry("js", "application/javascript"),
            entry("mjs", "application/javascript"),
            entry("css", "text/css"),
            entry("json", "application/json"),
            entry("map", "application/json"),
            entry("txt", "text/plain"),
            entry("png", "image/png"),
            entry("jpg", "image/jpeg"),
            entry("jpeg", "image/jpeg"),
            entry("gif", "image/gif"),
            entry("svg", "image/svg+xml"),
            entry("ico", "image/x-icon"),
            entry("webp", "image/webp"),
            entry("woff", "font/woff"),
            entry("woff2", "font/woff2"),
            entry("ttf", "font/ttf")
    );

    private static void serveFrontend(Context ctx) {
        String path = ctx.path();
        if (path.startsWith("/api/")) { // 未定义的 API 路径不参与前端 fallback
            ctx.status(404).result("Not Found");
            return;
        }
        String relative = path.startsWith("/") ? path.substring(1) : path;

        Path file = FRONTEND_DIR.resolve(relative).normalize();
        if (!file.startsWith(FRONTEND_DIR)) { // 路径穿越防护
            ctx.status(400).result("Bad Request");
            return;
        }

        if (Files.isRegularFile(file)) {
            serveStaticFile(ctx, file, false);
            return;
        }

        // 不存在的资源文件（含扩展名）直接 404，不 fallback 成 HTML
        String lastSegment = relative.substring(relative.lastIndexOf('/') + 1);
        if (lastSegment.contains(".")) {
            ctx.status(404).result("Not Found");
            return;
        }

        // 前端路由 fallback 到对应静态目录的 HTML
        Path html;
        if (relative.startsWith("room")) {
            html = FRONTEND_DIR.resolve("room/__fallback__/index.html");
        } else if (relative.startsWith("pool")) {
            html = FRONTEND_DIR.resolve("pool/__fallback__/index.html");
        } else {
            html = FRONTEND_DIR.resolve("index.html");
        }
        serveStaticFile(ctx, html, true);
    }

    private static void serveStaticFile(Context ctx, Path file, boolean html) {
        try {
            String name = file.getFileName().toString();
            int dot = name.lastIndexOf('.');
            String ext = dot >= 0 ? name.substring(dot + 1) : "";
            String contentType = html ? "text/html; charset=utf-8" : STATIC_MIME.getOrDefault(ext, "application/octet-stream");
            ctx.contentType(contentType);
            if (!html) {
                ctx.header("Cache-Control", "public, max-age=31536000, immutable");
            }
            InputStream in = Files.newInputStream(file);
            ctx.result(in);
        } catch (IOException e) {
            ctx.status(404).result("Not Found");
        }
    }

    // ===== 鉴权 =====

    private static void corsHeaders(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void authenticate(Context ctx) {
        if (ctx.method() == HandlerType.OPTIONS) {
            return;
        }
        if (ctx.path().equals("/api/v1/login")) {
            return;
        }

        String auth = ctx.header("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ApiException(401, "需要登录");
        }

        Integer userId = JwtService.parseUserId(auth.substring("Bearer ".length()));
        if (userId == null) {
            throw new ApiException(401, "需要登录");
        }
        ctx.attribute("userId", userId);
    }

    private static int requireUser(Context ctx) {
        Integer userId = ctx.attribute("userId");
        if (userId == null) {
            throw new ApiException(401, "需要登录");
        }
        return userId;
    }

    private static int requireAdmin(Context ctx) {
        int userId = requireUser(ctx);
        if (!AdminService.isAdmin(userId)) {
            throw new ApiException(403, "需要权限");
        }
        return userId;
    }

    private static void requireInRoom(int userId, LocalRoom room) {
        if (AdminService.isAdmin(userId)) {
            return;
        }
        PlayerManager.getPlayer(userId)
                .flatMap(p -> p.getRoom())
                .filter(r -> r == room)
                .orElseThrow(() -> new ApiException(403, "需要权限"));
    }

    // ===== 登录 =====

    private static void handleLogin(Context ctx) {
        LoginBody body = bodyOrNull(ctx, LoginBody.class);
        if (body == null || body.email() == null || body.password() == null) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }

        try {
            PhiraFetcher.LoginResult result = PhiraFetcher.POST_LOGIN.apply(body.email(), body.password());
            boolean isAdmin = AdminService.isAdmin(result.id());
            String jwt = JwtService.issueToken(result.id(), isAdmin);
            Server.getLogger().info("HTTP login: user {} isAdmin={}", result.id(), isAdmin);
            ctx.json(Map.of(
                    "ok", true,
                    "token", jwt,
                    "phira_token", result.token(),
                    "isAdmin", isAdmin,
                    "userId", result.id()
            ));
        } catch (IOException e) {
            throw new ApiException(400, "登录失败，请检查邮箱与密码");
        }
    }

    // ===== 房间 =====

    private static void handleRoomCreate(Context ctx) {
        requireAdmin(ctx);
        String roomId = ctx.pathParam("id");
        if (!ROOM_ID_PATTERN.matcher(roomId).matches()) {
            throw new ApiException(400, "房间名仅限字母、数字、-、_。");
        }
        if (RoomManager.findRoom(roomId) != null) {
            throw new ApiException(400, "房间已存在");
        }

        CreateRoomBody body = bodyOrNull(ctx, CreateRoomBody.class);
        String type = body == null || body.type() == null ? "local" : body.type();
        if (!"local".equals(type)) {
            throw new ApiException(400, "不支持的房间类型：" + type);
        }

        List<ChartPool.PoolSnapshot> pools;
        if (body == null || body.pools() == null || body.pools().isEmpty()) {
            pools = ChartPool.getDefaultPools();
            if (pools.isEmpty()) {
                throw new ApiException(400, "当前没有默认启用的谱池");
            }
        } else {
            pools = new ArrayList<>();
            for (int poolId : body.pools()) {
                ChartPool.PoolSnapshot pool = ChartPool.findPool(poolId);
                if (pool == null) {
                    throw new ApiException(400, "谱池不存在：" + poolId);
                }
                pools.add(pool);
            }
        }

        new LocalRoomBuilder()
                .host(false)
                .cycle(false)
                .chat(true)
                .autoDestroy(false)
                .pools(pools)
                .build(roomId);
        Server.getLogger().info("HTTP create room: {} pools={}", roomId, pools.stream().map(ChartPool.PoolSnapshot::id).toList());
        ctx.json(Map.of("ok", true));
    }

    private static void handleRoomUpdate(Context ctx) {
        requireAdmin(ctx);
        LocalRoom room = requireRoom(ctx.pathParam("id"));
        UpdateRoomBody body = bodyOrNull(ctx, UpdateRoomBody.class);
        if (body == null) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }

        LocalRoom.RoomSetting setting = room.getSetting();
        if (body.live() != null) {
            setting.setLive(body.live());
        }
        if (body.chatEnable() != null) {
            setting.setChat(body.chatEnable());
        }
        if (body.minPlayer() != null) {
            if (body.minPlayer() <= 0) {
                throw new ApiException(400, "minPlayer 必须大于 0");
            }
            setting.setMinPlayer(body.minPlayer());
        }
        if (body.maxPlayer() != null) {
            if (body.maxPlayer() <= 0) {
                throw new ApiException(400, "maxPlayer 必须大于 0");
            }
            setting.setMaxPlayer(body.maxPlayer());
        }
        if (body.selectCountdown() != null) {
            if (body.selectCountdown() < 10) {
                throw new ApiException(400, "selectCountdown 必须大于等于 10");
            }
            setting.setSelectChartCountdownSeconds(body.selectCountdown());
        }
        if (body.readyCountdown() != null) {
            if (body.readyCountdown() <= 0) {
                throw new ApiException(400, "readyCountdown 必须大于 0");
            }
            setting.setReadyCountdownSeconds(body.readyCountdown());
        }
        if (body.forceFinish() != null) {
            if (body.forceFinish() <= 0) {
                throw new ApiException(400, "forceFinish 必须大于 0");
            }
            setting.setForceFinishSeconds(body.forceFinish());
        }
        if (body.interval() != null) {
            if (body.interval() <= 0) {
                throw new ApiException(400, "interval 必须大于 0");
            }
            setting.setRefreshIntervalRounds(body.interval());
        }
        Server.getLogger().info("HTTP update room: {} fields updated", room.getRoomId());
        ctx.json(Map.of("ok", true));
    }

    private static void handleRoomGet(Context ctx) {
        int userId = requireUser(ctx);
        LocalRoom room = requireRoom(ctx.pathParam("id"));
        requireInRoom(userId, room);
        ctx.json(Map.of("ok", true, "info", roomSnapshot(room)));
    }

    private static void handleRoomList(Context ctx) {
        int userId = requireUser(ctx);
        if (AdminService.isAdmin(userId)) {
            List<Map<String, Object>> rooms = RoomManager.getAllRooms().stream()
                    .map(room -> roomSnapshot((LocalRoom) room))
                    .toList();
            ctx.json(Map.of("ok", true, "rooms", rooms));
            return;
        }

        List<Map<String, Object>> rooms = PlayerManager.getPlayer(userId)
                .flatMap(p -> p.getRoom())
                .map(room -> List.of(roomSnapshot((LocalRoom) room)))
                .orElseGet(List::of);
        ctx.json(Map.of("ok", true, "rooms", rooms));
    }

    private static void handleRoomDelete(Context ctx) {
        requireAdmin(ctx);
        LocalRoom room = requireRoom(ctx.pathParam("id"));
        if (!room.getView().getPlayers().isEmpty() || !room.getView().getMonitors().isEmpty()) {
            throw new ApiException(400, "房间非空，无法删除");
        }
        RoomManager.removeRoom(room.getRoomId());
        Server.getLogger().info("HTTP delete room: {}", room.getRoomId());
        ctx.json(Map.of("ok", true));
    }

    private static void handleRoomEnd(Context ctx) {
        requireAdmin(ctx);
        LocalRoom room = requireRoom(ctx.pathParam("id"));
        if (!(room.getView().getState() instanceof RoomPlaying state)) {
            throw new ApiException(400, "房间不在游戏中");
        }
        state.forceFinishByServer();
        Server.getLogger().info("HTTP force end room: {}", room.getRoomId());
        ctx.json(Map.of("ok", true));
    }

    private static void handleRoomPoolGet(Context ctx) {
        int userId = requireUser(ctx);
        LocalRoom room = requireRoom(ctx.pathParam("id"));
        requireInRoom(userId, room);
        ctx.json(Map.of("ok", true, "pool", poolStatus(room)));
    }

    private static void handleRoomPoolSwitch(Context ctx) {
        requireAdmin(ctx);
        LocalRoom room = requireRoom(ctx.pathParam("id"));
        SwitchPoolBody body = bodyOrNull(ctx, SwitchPoolBody.class);
        if (body == null) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }
        try {
            room.getChartPool().switchPool(body.poolId());
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, e.getMessage());
        }
        Server.getLogger().info("HTTP switch room {} pending pool to {}", room.getRoomId(), body.poolId());
        ctx.json(Map.of("ok", true));
    }

    private static void handleRoomPoolFavorite(Context ctx) {
        requireAdmin(ctx);
        LocalRoom room = requireRoom(ctx.pathParam("id"));
        FavoriteBody body = requireFavoriteBody(ctx);
        room.getChartPool().setFavorite(body.favoriteId());
        Server.getLogger().info("HTTP room {} pool favorite set to {}", room.getRoomId(), body.favoriteId());
        ctx.json(Map.of("ok", true));
    }

    // ===== 全局谱池管理 =====

    private static void handlePoolList(Context ctx) {
        requireAdmin(ctx);
        List<Map<String, Object>> pools = ChartPool.listPools().stream()
                .map(ApiServer::poolSnapshot)
                .toList();
        ctx.json(Map.of("ok", true, "pools", pools));
    }

    private static void handlePoolCreate(Context ctx) {
        requireAdmin(ctx);
        PoolCreateBody body = bodyOrNull(ctx, PoolCreateBody.class);
        if (body == null || body.chartIds() == null || body.chartIds().isEmpty()) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }
        try {
            ChartPool.addPool(body.id(), body.chartIds());
        } catch (Exception e) {
            throw new ApiException(400, e.getMessage());
        }
        Server.getLogger().info("HTTP create pool: {} charts={}", body.id(), body.chartIds());
        ctx.json(Map.of("ok", true));
    }

    private static void handlePoolRemove(Context ctx) {
        requireAdmin(ctx);
        int poolId = pathInt(ctx, "id");
        try {
            ChartPool.removePool(poolId);
        } catch (Exception e) {
            throw new ApiException(400, e.getMessage());
        }
        Server.getLogger().info("HTTP remove pool: {}", poolId);
        ctx.json(Map.of("ok", true));
    }

    private static void handlePoolChartAdd(Context ctx) {
        requireAdmin(ctx);
        int poolId = pathInt(ctx, "id");
        ChartAddBody body = bodyOrNull(ctx, ChartAddBody.class);
        if (body == null) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }
        try {
            ChartPool.addChart(poolId, body.chartId());
        } catch (Exception e) {
            throw new ApiException(400, e.getMessage());
        }
        Server.getLogger().info("HTTP add chart {} to pool {}", body.chartId(), poolId);
        ctx.json(Map.of("ok", true));
    }

    private static void handlePoolChartRemove(Context ctx) {
        requireAdmin(ctx);
        int poolId = pathInt(ctx, "id");
        int chartId = pathInt(ctx, "chartId");
        try {
            ChartPool.removeChart(poolId, chartId);
        } catch (Exception e) {
            throw new ApiException(400, e.getMessage());
        }
        Server.getLogger().info("HTTP remove chart {} from pool {}", chartId, poolId);
        ctx.json(Map.of("ok", true));
    }

    private static void handlePoolFavorite(Context ctx) {
        requireAdmin(ctx);
        int poolId = pathInt(ctx, "id");
        FavoriteBody body = requireFavoriteBody(ctx);
        ChartPool.setFavoriteId(poolId, body.favoriteId());
        Server.getLogger().info("HTTP pool {} favorite set to {}", poolId, body.favoriteId());
        ctx.json(Map.of("ok", true));
    }

    private static void handlePoolDefault(Context ctx) {
        requireAdmin(ctx);
        int poolId = pathInt(ctx, "id");
        PoolDefaultBody body = bodyOrNull(ctx, PoolDefaultBody.class);
        if (body == null) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }
        ChartPool.setDefaultFlag(poolId, body.enabled());
        Server.getLogger().info("HTTP pool {} default set to {}", poolId, body.enabled());
        ctx.json(Map.of("ok", true));
    }

    // ===== 辅助 =====

    private static LocalRoom requireRoom(String roomId) {
        Room room = RoomManager.findRoom(roomId);
        if (room == null) {
            throw new ApiException(404, "欲请求的内容不存在");
        }
        return (LocalRoom) room;
    }

    private static FavoriteBody requireFavoriteBody(Context ctx) {
        String body = ctx.body();
        if (body == null || body.isBlank()) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("favoriteId")) {
                throw new ApiException(400, "请检查是否传入了错误的格式");
            }
            Integer favoriteId = json.get("favoriteId").isJsonNull() ? null : json.get("favoriteId").getAsInt();
            return new FavoriteBody(favoriteId);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }
    }

    private static int pathInt(Context ctx, String param) {
        try {
            return Integer.parseInt(ctx.pathParam(param));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "参数格式错误");
        }
    }

    private static <T> T bodyOrNull(Context ctx, Class<T> clazz) {
        String body = ctx.body();
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            throw new ApiException(400, "请检查是否传入了错误的格式");
        }
    }

    private static Map<String, Object> roomSnapshot(LocalRoom room) {
        RoomSnapshot view = room.getView();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("roomId", view.getRoomId());
        info.put("state", stateName(view.getState()));
        info.put("live", view.isLive());
        info.put("locked", view.isLocked());
        info.put("cycle", view.isCycle());
        info.put("host", view.getHost());
        info.put("chart", chartInfo(view.getState().getChart()));
        info.put("type", "local");
        info.put("config", roomConfig(room.getSetting()));
        info.put("pool", poolStatus(room));
        info.put("players", view.getPlayers().stream()
                .map(p -> Map.of("id", p.getId(), "name", p.getName()))
                .toList());
        info.put("monitors", view.getMonitors().stream()
                .map(p -> Map.of("id", p.getId(), "name", p.getName()))
                .toList());
        return info;
    }

    private static Map<String, Object> chartInfo(ChartInfo chart) {
        if (chart == null) {
            return null;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", chart.getId());
        info.put("name", chart.getName());
        info.put("level", chart.getLevel());
        info.put("difficulty", chart.getDifficulty());
        info.put("charter", chart.getCharter());
        info.put("composer", chart.getComposer());
        info.put("illustrator", chart.getIllustrator());
        info.put("description", chart.getDescription());
        info.put("ranked", chart.isRanked());
        info.put("reviewed", chart.isReviewed());
        info.put("stable", chart.isStable());
        info.put("stableRequest", chart.isStableRequest());
        info.put("illustration", chart.getIllustration());
        info.put("preview", chart.getPreview());
        info.put("file", chart.getFile());
        info.put("uploader", chart.getUploader());
        info.put("tags", chart.getTags() == null ? List.of() : List.of(chart.getTags()));
        info.put("rating", chart.getRating());
        info.put("ratingCount", chart.getRatingCount());
        info.put("created", chart.getCreated());
        info.put("updated", chart.getUpdated());
        info.put("chartUpdated", chart.getChartUpdated());
        return info;
    }

    private static String stateName(RoomGameState state) {
        if (state instanceof RoomPlaying) {
            return "Playing";
        }
        if (state instanceof RoomWaitForReady) {
            return "WaitForReady";
        }
        return "SelectChart";
    }

    private static Map<String, Object> roomConfig(LocalRoom.RoomSetting setting) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("minPlayer", setting.getMinPlayer());
        config.put("maxPlayer", setting.getMaxPlayer());
        config.put("selectCountdown", setting.getSelectChartCountdownSeconds());
        config.put("readyCountdown", setting.getReadyCountdownSeconds());
        config.put("forceFinish", setting.getForceFinishSeconds());
        config.put("interval", setting.getRefreshIntervalRounds());
        return config;
    }

    private static Map<String, Object> poolStatus(LocalRoom room) {
        RoomChartPool pool = room.getChartPool();
        ChartPool.PoolSnapshot current = pool.getCurrentPoolSnapshot();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("currentPool", poolSnapshot(current));
        status.put("pools", pool.getPools().stream().map(ApiServer::poolSnapshot).toList());
        status.put("pendingPoolId", pool.getPendingPoolId());
        status.put("favoriteId", current.favoriteId());
        status.put("finishedRoundsSinceRefresh", pool.getFinishedRoundsSinceRefresh());
        status.put("refreshIntervalRounds", room.getSetting().getRefreshIntervalRounds());
        return status;
    }

    private static Map<String, Object> poolSnapshot(ChartPool.PoolSnapshot pool) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", pool.id());
        snapshot.put("favoriteId", pool.favoriteId());
        snapshot.put("default", pool.defaultFlag());
        snapshot.put("chartIds", pool.chartIds());
        return snapshot;
    }

    private static final class ApiException extends RuntimeException {
        private final int status;
        private final String reason;

        private ApiException(int status, String reason) {
            super(reason);
            this.status = status;
            this.reason = reason;
        }
    }

    // ===== DTO =====
    // 注意：Gson 使用 LOWER_CASE_WITH_UNDERSCORES 命名策略，record 组件名会被转换为下划线键，
    // 因此驼峰字段必须用 @SerializedName 显式声明文档约定的 JSON 键名。

    public record LoginBody(String email, String password) {
    }

    public record CreateRoomBody(String type, List<Integer> pools) {
    }

    public record UpdateRoomBody(
            Boolean live, Boolean lock,
            @SerializedName("chatEnable") Boolean chatEnable,
            @SerializedName("minPlayer") Integer minPlayer,
            @SerializedName("maxPlayer") Integer maxPlayer,
            @SerializedName("selectCountdown") Integer selectCountdown,
            @SerializedName("readyCountdown") Integer readyCountdown,
            @SerializedName("forceFinish") Integer forceFinish,
            Integer interval
    ) {
    }

    public record SwitchPoolBody(@SerializedName("poolId") int poolId) {
    }

    public record FavoriteBody(@SerializedName("favoriteId") Integer favoriteId) {
    }

    public record PoolCreateBody(int id, @SerializedName("chartIds") List<Integer> chartIds) {
    }

    public record ChartAddBody(@SerializedName("chartId") int chartId) {
    }

    public record PoolDefaultBody(boolean enabled) {
    }
}
