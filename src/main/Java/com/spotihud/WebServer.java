package com.spotihud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class WebServer {
    public static final int PORT = 8888;

    public static void start() {
        try {
            InetAddress lo = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
            HttpServer server = HttpServer.create(new InetSocketAddress(lo, PORT), 0);

            server.createContext("/", ex -> {
                byte[] page;
                try (InputStream in = WebServer.class.getResourceAsStream("/web/index.html")) {
                    page = in.readAllBytes();
                }
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                send(ex, 200, page);
            });

            server.createContext("/cmd", ex -> {
                send(ex, 200, SpotifyState.takeCommand().getBytes(StandardCharsets.UTF_8));
            });

            server.createContext("/state", ex -> {
                try {
                    String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject o = JsonParser.parseString(body).getAsJsonObject();
                    boolean active = o.has("active") && o.get("active").getAsBoolean();
                    SpotifyState.active = active;
                    if (active) {
                        SpotifyState.title = o.get("title").getAsString();
                        SpotifyState.artist = o.get("artist").getAsString();
                        SpotifyState.playing = o.get("playing").getAsBoolean();
                        SpotifyState.progressMs = o.get("progress").getAsLong();
                        SpotifyState.durationMs = o.get("duration").getAsLong();
                        SpotifyState.updatedAt = System.currentTimeMillis();
                        String cover = o.get("cover").getAsString();
                        if (!cover.equals(SpotifyState.coverUrl)) {
                            SpotifyState.coverUrl = cover;
                            CoverLoader.load(cover);
                        }
                    }
                } catch (Exception ignored) {
                }
                ex.sendResponseHeaders(204, -1);
                ex.close();
            });

            server.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "spotihud-web");
                t.setDaemon(true);
                return t;
            }));
            server.start();
        } catch (Exception e) {
            System.err.println("[Spoti-Hud] Web server could not start: " + e);
        }
    }

    private static void send(HttpExchange ex, int code, byte[] data) throws java.io.IOException {
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private WebServer() {}
}
