package top.rymc.phira.test;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import top.rymc.phira.main.util.GsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockPhiraServer {

    private static final Gson GSON = GsonUtil.getGson();
    private HttpServer server;
    private int port;

    private final Map<Integer, Map<String, Object>> records = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Object>> charts = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Object>> users = new ConcurrentHashMap<>();

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/me", this::handleMe);
        server.createContext("/user/", this::handleUser);
        server.createContext("/chart/", this::handleChart);
        server.createContext("/record/", this::handleRecord);
        server.setExecutor(null);
        server.start();
        port = server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public String getBaseUrl() {
        return "http://localhost:" + port + "/";
    }

    public void addRecord(int id, int playerId, int chartId, int score, float accuracy, boolean fullCombo) {
        Map<String, Object> record = new ConcurrentHashMap<>();
        record.put("id", id);
        record.put("player", playerId);
        record.put("chart", chartId);
        record.put("score", score);
        record.put("accuracy", accuracy);
        record.put("perfect", 500);
        record.put("good", 10);
        record.put("bad", 0);
        record.put("miss", 0);
        record.put("speed", 1.0f);
        record.put("max_combo", 510);
        record.put("best", true);
        record.put("best_std", true);
        record.put("mods", 0);
        record.put("full_combo", fullCombo);
        record.put("time", OffsetDateTime.now(ZoneOffset.UTC));
        record.put("std", 0.5f);
        record.put("std_score", score * 0.4f);
        records.put(id, record);
    }

    public void addChart(int id, String name, String charter, String level) {
        Map<String, Object> chart = new ConcurrentHashMap<>();
        chart.put("id", id);
        chart.put("name", name);
        chart.put("level", level);
        chart.put("difficulty", 3.0f);
        chart.put("charter", charter);
        chart.put("composer", "Test Composer");
        chart.put("illustrator", "Test Illustrator");
        chart.put("description", "Test Description");
        chart.put("ranked", true);
        chart.put("reviewed", true);
        chart.put("stable", true);
        chart.put("stable_request", false);
        chart.put("illustration", "");
        chart.put("preview", "");
        chart.put("file", "");
        chart.put("uploader", 1);
        chart.put("tags", new String[]{});
        chart.put("rating", 4.5f);
        chart.put("rating_count", 100);
        chart.put("created", OffsetDateTime.now(ZoneOffset.UTC));
        chart.put("updated", OffsetDateTime.now(ZoneOffset.UTC));
        chart.put("chart_updated", OffsetDateTime.now(ZoneOffset.UTC));
        charts.put(id, chart);
    }

    public void addUser(int id, String name, String email) {
        Map<String, Object> user = new ConcurrentHashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("avatar", "");
        user.put("language", "zh-CN");
        user.put("bio", "Test Bio");
        user.put("exp", 100);
        user.put("rks", 15.5);
        user.put("joined", OffsetDateTime.now(ZoneOffset.UTC));
        user.put("last_login", OffsetDateTime.now(ZoneOffset.UTC));
        user.put("roles", 1);
        user.put("banned", false);
        user.put("login_banned", false);
        user.put("follower_count", 50);
        user.put("following_count", 30);
        user.put("email", email);
        users.put(id, user);
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        Map<String, Object> user = createDefaultUser(1, "testuser", "test@example.com");
        sendJsonResponse(exchange, 200, user);
    }

    private void handleUser(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int userId = extractId(path, "/user/");

        Map<String, Object> user = users.getOrDefault(userId, createDefaultUser(userId, "user" + userId, "user" + userId + "@example.com"));
        sendJsonResponse(exchange, 200, user);
    }

    private void handleChart(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int chartId = extractId(path, "/chart/");

        Map<String, Object> chart = charts.getOrDefault(chartId, createDefaultChart(chartId, "Chart " + chartId, "Test Charter", "5"));
        sendJsonResponse(exchange, 200, chart);
    }

    private void handleRecord(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int recordId = extractId(path, "/record/");

        Map<String, Object> record = records.getOrDefault(recordId, createDefaultRecord(recordId, 1, 1, 1000000, 99.5f, true));
        sendJsonResponse(exchange, 200, record);
    }

    private int extractId(String path, String prefix) {
        String idStr = path.substring(prefix.length());
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Map<String, Object> createDefaultUser(int id, String name, String email) {
        Map<String, Object> user = new ConcurrentHashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("avatar", "");
        user.put("language", "zh-CN");
        user.put("bio", "Test Bio");
        user.put("exp", 100);
        user.put("rks", 15.5);
        user.put("joined", OffsetDateTime.now(ZoneOffset.UTC));
        user.put("last_login", OffsetDateTime.now(ZoneOffset.UTC));
        user.put("roles", 1);
        user.put("banned", false);
        user.put("login_banned", false);
        user.put("follower_count", 50);
        user.put("following_count", 30);
        user.put("email", email);
        return user;
    }

    private Map<String, Object> createDefaultChart(int id, String name, String charter, String level) {
        Map<String, Object> chart = new ConcurrentHashMap<>();
        chart.put("id", id);
        chart.put("name", name);
        chart.put("level", level);
        chart.put("difficulty", 3.0f);
        chart.put("charter", charter);
        chart.put("composer", "Test Composer");
        chart.put("illustrator", "Test Illustrator");
        chart.put("description", "Test Description");
        chart.put("ranked", true);
        chart.put("reviewed", true);
        chart.put("stable", true);
        chart.put("stable_request", false);
        chart.put("illustration", "");
        chart.put("preview", "");
        chart.put("file", "");
        chart.put("uploader", 1);
        chart.put("tags", new String[]{});
        chart.put("rating", 4.5f);
        chart.put("rating_count", 100);
        chart.put("created", OffsetDateTime.now(ZoneOffset.UTC));
        chart.put("updated", OffsetDateTime.now(ZoneOffset.UTC));
        chart.put("chart_updated", OffsetDateTime.now(ZoneOffset.UTC));
        return chart;
    }

    private Map<String, Object> createDefaultRecord(int id, int playerId, int chartId, int score, float accuracy, boolean fullCombo) {
        Map<String, Object> record = new ConcurrentHashMap<>();
        record.put("id", id);
        record.put("player", playerId);
        record.put("chart", chartId);
        record.put("score", score);
        record.put("accuracy", accuracy);
        record.put("perfect", 500);
        record.put("good", 10);
        record.put("bad", 0);
        record.put("miss", 0);
        record.put("speed", 1.0f);
        record.put("max_combo", 510);
        record.put("best", true);
        record.put("best_std", true);
        record.put("mods", 0);
        record.put("full_combo", fullCombo);
        record.put("time", OffsetDateTime.now(ZoneOffset.UTC));
        record.put("std", 0.5f);
        record.put("std_score", score * 0.4f);
        return record;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = GSON.toJson(data);
        sendResponse(exchange, statusCode, json);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
