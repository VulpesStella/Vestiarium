package io.github.vulpesstella.vestiarium.skin;

import io.github.vulpesstella.vestiarium.Vestiarium;
import io.github.vulpesstella.vestiarium.config.VestiariumConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * Content addressed disk cache for downloaded textures. URLs supplied by
 * Mojang and Yggdrasil servers are content hashes, so a URL maps to one image
 * forever in practice. Direct URLs use the cache age limit.
 */
public final class TextureCache {
    private TextureCache() {
    }

    public static File file(String url, String kind) {
        if (url == null || url.length() == 0 || VestiariumConfig.cacheDays() <= 0) {
            return null;
        }
        File directory = new File(VestiariumConfig.cacheDir(), kind);
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            return null;
        }
        return new File(directory, sha1(url) + ".png");
    }

    public static boolean isFresh(File file, int days) {
        if (file == null || !file.isFile() || days <= 0) {
            return false;
        }
        long age = System.currentTimeMillis() - file.lastModified();
        return age >= 0 && age <= days * 24L * 60L * 60L * 1000L;
    }

    public static BufferedImage read(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            return ImageIO.read(file);
        } catch (Throwable t) {
            Vestiarium.debug("Could not read texture cache " + file + ": " + t);
            return null;
        }
    }

    public static void write(File file, BufferedImage image) {
        if (file == null || image == null || VestiariumConfig.cacheDays() <= 0) {
            return;
        }
        File parent = file.getParentFile();
        if (parent == null) {
            return;
        }
        if (!parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            return;
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        try {
            if (ImageIO.write(image, "png", temporary)) {
                if (file.exists() && !file.delete()) {
                    return;
                }
                if (!temporary.renameTo(file)) {
                    temporary.delete();
                }
            } else {
                temporary.delete();
            }
        } catch (Throwable t) {
            temporary.delete();
            Vestiarium.debug("Could not write texture cache " + file + ": " + t);
        }
    }

    public static String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                int v = b & 0xff;
                if (v < 16) {
                    out.append('0');
                }
                out.append(Integer.toHexString(v));
            }
            return out.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    static void createDirectories(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Could not create " + directory);
        }
    }
}
