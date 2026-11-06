package io.github.vulpesstella.vestiarium.skin;

import io.github.vulpesstella.vestiarium.Vestiarium;
import io.github.vulpesstella.vestiarium.config.VestiariumConfig;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Downloads a single skin or cape image on a daemon thread and uploads it when
 * the render thread next binds the texture. The vanilla ThreadDownloadImageData
 * always creates an ImageBufferDownload (which crops everything to 64x32), so
 * Vestiarium replaces that part with its own downloader.
 */
public class DownloadedTexture extends ThreadDownloadImageData {
    public interface Listener {
        void onFailed();
    }

    private final String url;
    private final IImageBuffer buffer;
    private final BufferedImage fallback;
    private final File cacheFile;
    private final boolean allowLocal;
    private final Listener listener;

    private volatile BufferedImage pending;
    private volatile boolean uploaded;
    private volatile boolean started;

    public DownloadedTexture(String url, IImageBuffer buffer, BufferedImage fallback,
                            File cacheFile, boolean allowLocal, Listener listener) {
        super(url == null ? "" : url, null, buffer);
        this.url = url;
        this.buffer = buffer;
        this.fallback = fallback;
        this.cacheFile = cacheFile;
        this.allowLocal = allowLocal;
        this.listener = listener;
        this.pending = fallback;
    }

    @Override
    public void loadTexture(ResourceManager resourceManager) {
        if (started) {
            return;
        }
        started = true;
        Vestiarium.debug("Texture start: " + url + " -> " + cacheFile);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                download();
            }
        }, "Vestiarium texture " + (url == null ? "fallback" : url));
        thread.setDaemon(true);
        thread.start();
    }

    private void download() {
        BufferedImage image = null;
        boolean fromNetwork = false;
        try {
            if (TextureCache.isFresh(cacheFile, VestiariumConfig.cacheDays())) {
                image = TextureCache.read(cacheFile);
            }
            if (image == null) {
                fromNetwork = url != null && !url.toLowerCase().startsWith("file:");
                byte[] bytes = SkinHttp.getImage(url, allowLocal);
                image = ImageIO.read(new ByteArrayInputStream(bytes));
            }
            if (image == null) {
                throw new IllegalStateException("Not a readable image");
            }
            if (image.getWidth() > 4096 || image.getHeight() > 4096
                    || (long) image.getWidth() * image.getHeight() > 16L * 1024L * 1024L) {
                throw new IllegalStateException("Image is too large: " + image.getWidth() + "x" + image.getHeight());
            }
            BufferedImage processed = buffer == null ? image : buffer.parseUserSkin(image);
            if (processed == null) {
                processed = image;
            }
            Vestiarium.debug("Texture parsed: " + url + " -> " + processed.getWidth() + "x" + processed.getHeight()
                    + " sample=" + Integer.toHexString(processed.getRGB(8, 8)));
            setPending(processed);
            if (fromNetwork) {
                TextureCache.write(cacheFile, processed);
            }
        } catch (Throwable t) {
            Vestiarium.logger().warning("Texture download failed for " + url + ": " + t);
            if (image == null) {
                image = TextureCache.read(cacheFile);
            }
            if (image != null && pending == null) {
                setPending(image);
            }
            if (listener != null) {
                listener.onFailed();
            }
        }
    }

    private synchronized void setPending(BufferedImage image) {
        this.pending = image;
    }

    @Override
    public int getGlTextureId() {
        int id = super.getGlTextureId();
        BufferedImage image;
        synchronized (this) {
            image = this.pending;
            this.pending = null;
        }
        if (image != null) {
            TextureUtil.uploadTextureImage(id, image);
            this.uploaded = true;
            Vestiarium.debug("Texture upload: " + url + " id=" + id + " "
                    + image.getWidth() + "x" + image.getHeight()
                    + " sample=" + Integer.toHexString(image.getRGB(8, 8)));
        }
        return id;
    }

    @Override
    public boolean isTextureUploaded() {
        getGlTextureId();
        return uploaded;
    }
}
