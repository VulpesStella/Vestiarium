package io.github.vulpesstella.vestiarium.skin;

import net.minecraft.client.renderer.IImageBuffer;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Normalises capes to the 64x32 texture the 1.6.4 cape model expects. The
 * legacy 22x17 cape format is padded into the top left corner; larger capes
 * are cropped or scaled.
 */
public final class CapeImageBuffer implements IImageBuffer {
    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        if (image == null) {
            return null;
        }
        BufferedImage output = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if (image.getWidth() == 22 && image.getHeight() == 17) {
                graphics.drawImage(image, 0, 0, null);
            } else if (image.getWidth() == 64 && image.getHeight() == 64) {
                graphics.drawImage(image, 0, 0, 64, 32, 0, 0, 64, 32, null);
            } else if (image.getWidth() == 64 && image.getHeight() == 32) {
                graphics.drawImage(image, 0, 0, null);
            } else {
                graphics.drawImage(image, 0, 0, 64, 32, null);
            }
        } finally {
            graphics.dispose();
        }
        return output;
    }
}
