package com.spotihud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;

public final class CoverLoader {
    public static final int SIZE = 64;
    public static volatile boolean ready = false;

    public static void load(String url) {
        Thread t = new Thread(() -> {
            try {
                BufferedImage src = ImageIO.read(URI.create(url).toURL());
                if (src == null) return;
                BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, SIZE, SIZE, null);
                g.dispose();

                NativeImage ni = new NativeImage(SIZE, SIZE, false);
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        ni.setColorArgb(x, y, img.getRGB(x, y));
                    }
                }
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    mc.getTextureManager().registerTexture(
                            SpotiHudClient.COVER_ID,
                            new NativeImageBackedTexture(() -> "spotihud_cover", ni));
                    ready = true;
                });
            } catch (Exception e) {
                System.err.println("[Spoti-Hud] Cover failed: " + e);
            }
        }, "spotihud-cover");
        t.setDaemon(true);
        t.start();
    }

    private CoverLoader() {}
}
