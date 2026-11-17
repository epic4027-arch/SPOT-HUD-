package com.spotihud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class SpotiHudClient implements ClientModInitializer {
    public static final Identifier COVER_ID = Identifier.of("spotihud", "cover");

    private static KeyBinding prevKey, toggleKey, nextKey;

    @Override
    public void onInitializeClient() {
        WebServer.start();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("spotihud", "main"));
        prevKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.spotihud.prev", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_COMMA, cat));
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.spotihud.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SLASH, cat));
        nextKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.spotihud.next", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, cat));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (prevKey.wasPressed()) SpotifyState.queue("prev");
            while (toggleKey.wasPressed()) SpotifyState.queue("toggle");
            while (nextKey.wasPressed()) SpotifyState.queue("next");
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("spotihud", "hud"),
                SpotiHudClient::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        if (!SpotifyState.active) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        int x = 6, y = 6, w = 170, h = 44;
        ctx.fill(x, y, x + w, y + h, 0xA0000000);

        int textX = x + 6;
        if (CoverLoader.ready) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, COVER_ID, x + 4, y + 4, 0f, 0f, 36, 36, 36, 36);
            textX = x + 44;
        }

        int maxW = x + w - textX - 4;
        ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(SpotifyState.title, maxW), textX, y + 6, 0xFFFFFFFF);
        ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(SpotifyState.artist, maxW), textX, y + 18, 0xFFB3B3B3);

        long pos = SpotifyState.progressMs;
        if (SpotifyState.playing) pos += System.currentTimeMillis() - SpotifyState.updatedAt;
        long dur = Math.max(1, SpotifyState.durationMs);
        float frac = Math.min(1f, Math.max(0f, (float) pos / dur));
        int barW = x + w - textX - 4;
        ctx.fill(textX, y + 33, textX + barW, y + 35, 0xFF444444);
        ctx.fill(textX, y + 33, textX + (int) (barW * frac), y + 35, SpotifyState.playing ? 0xFF1DB954 : 0xFF888888);
    }
}
