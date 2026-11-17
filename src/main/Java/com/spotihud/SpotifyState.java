package com.spotihud;

public final class SpotifyState {
    public static volatile boolean active = false;
    public static volatile boolean playing = false;
    public static volatile String title = "";
    public static volatile String artist = "";
    public static volatile String coverUrl = "";
    public static volatile long progressMs = 0;
    public static volatile long durationMs = 0;
    public static volatile long updatedAt = 0;

    private static String command = "";

    public static synchronized void queue(String c) { command = c; }

    public static synchronized String takeCommand() {
        String c = command;
        command = "";
        return c;
    }

    private SpotifyState() {}
}
