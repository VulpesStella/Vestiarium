package io.github.vulpesstella.vestiarium.skin;

import net.minecraft.client.renderer.IImageBuffer;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Normalises downloaded skins to 64x32 or 64x64 and decides whether a 64x64
 * skin uses the slim layout.
 *
 * The slim test looks at the areas that are only used by the 4 pixel wide
 * classic arms. A classic skin keeps those areas opaque; a slim skin leaves
 * them transparent. Profile metadata, when present, takes precedence.
 */
public final class SkinImageBuffer implements IImageBuffer {
    private static final int ALPHA_THRESHOLD = 32;

    private final String key;
    private final SkinModel hint;

    public SkinImageBuffer(String key, SkinModel hint) {
        this.key = key;
        this.hint = hint;
    }

    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        if (image == null) {
            return null;
        }
        BufferedImage normalized = normalize(image);
        SkinModel model;
        if (normalized.getHeight() <= 32) {
            model = SkinModel.LEGACY;
        } else if (hint == SkinModel.SLIM || hint == SkinModel.CLASSIC) {
            model = hint;
        } else {
            model = isSlim(normalized) ? SkinModel.SLIM : SkinModel.CLASSIC;
        }
        SkinManager.onSkinParsed(key, model);
        return normalized;
    }

    private static BufferedImage normalize(BufferedImage image) {
        boolean modern = image.getHeight() >= image.getWidth();
        int targetWidth = 64;
        int targetHeight = modern ? 64 : 32;
        if (image.getWidth() == targetWidth && image.getHeight() == targetHeight
                && image.getType() == BufferedImage.TYPE_INT_ARGB) {
            return image;
        }
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    static boolean isSlim(BufferedImage image) {
        return transparent(image, 54, 20, 56, 32)
                && transparent(image, 54, 36, 56, 48)
                && transparent(image, 46, 52, 48, 64)
                && transparent(image, 62, 52, 64, 64);
    }

    private static boolean transparent(BufferedImage image, int x0, int y0, int x1, int y1) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alpha >= ALPHA_THRESHOLD) {
                    return false;
                }
            }
        }
        return true;
    }
}
