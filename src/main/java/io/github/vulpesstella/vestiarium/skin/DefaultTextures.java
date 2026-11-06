package io.github.vulpesstella.vestiarium.skin;

import io.github.vulpesstella.vestiarium.Vestiarium;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Bundled fallback used until a player's skin finishes downloading. Keeping
 * the fallback at 64x32 means the legacy model is always a valid pairing.
 */
public final class DefaultTextures {
    private static BufferedImage skin;

    private DefaultTextures() {
    }

    public static synchronized BufferedImage skin() {
        if (skin != null) {
            return skin;
        }
        InputStream in = null;
        try {
            in = DefaultTextures.class.getResourceAsStream("/assets/vestiarium/textures/entity/steve.png");
            if (in != null) {
                skin = ImageIO.read(in);
            }
        } catch (Throwable t) {
            Vestiarium.debug("Could not load the bundled fallback skin: " + t);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
        if (skin == null) {
            skin = createFallback();
        }
        return skin;
    }

    private static BufferedImage createFallback() {
        BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(0x8a5a3b));
            graphics.fillRect(0, 0, 64, 32);
            graphics.setColor(new Color(0x2b2b2b));
            graphics.fillRect(0, 0, 8, 8);
            graphics.setColor(new Color(0xffffff));
            graphics.fillRect(8, 8, 8, 8);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
