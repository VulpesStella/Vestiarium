package io.github.vulpesstella.vestiarium.skin;

import io.github.vulpesstella.vestiarium.Vestiarium;
import io.github.vulpesstella.vestiarium.config.VestiariumConfig;

import java.io.File;
import java.util.Locale;

/**
 * Reads skin and cape PNGs from config/vestiarium/skins and
 * config/vestiarium/capes. A file named {@code name.slim.png} selects the slim
 * model explicitly.
 */
public final class LocalSkinProvider implements SkinProvider {
    @Override
    public PlayerSkin resolve(String username) {
        String safe = safeName(username);
        if (safe == null) {
            return null;
        }
        File skinFile = null;
        boolean slim = false;
        File slimFile = find(VestiariumConfig.skinDir(), safe, ".slim.png");
        if (slimFile != null) {
            skinFile = slimFile;
            slim = true;
        } else {
            skinFile = find(VestiariumConfig.skinDir(), safe, ".png");
        }
        File capeFile = find(VestiariumConfig.capeDir(), safe, ".png");
        if (skinFile == null && capeFile == null) {
            return null;
        }
        SkinModel model = slim ? SkinModel.SLIM : null;
        PlayerSkin skin = new PlayerSkin(username, fileUrl(skinFile), fileUrl(capeFile), model, true);
        Vestiarium.debug("Local skin for " + username + ": " + skin.skinUrl + " / " + skin.capeUrl);
        return skin;
    }

    public static String capeUrl(String username) {
        String safe = safeName(username);
        if (safe == null) {
            return null;
        }
        return fileUrl(find(VestiariumConfig.capeDir(), safe, ".png"));
    }

    private static File find(File directory, String name, String suffix) {
        if (directory == null || !directory.isDirectory()) {
            return null;
        }
        File exact = new File(directory, name + suffix);
        if (exact.isFile()) {
            return exact;
        }
        String wanted = (name + suffix).toLowerCase(Locale.ROOT);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).equals(wanted)) {
                    return file;
                }
            }
        }
        return null;
    }

    private static String fileUrl(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            return file.toURI().toURL().toExternalForm();
        } catch (Throwable t) {
            return null;
        }
    }

    static String safeName(String username) {
        if (username == null || username.length() == 0) {
            return null;
        }
        StringBuilder out = new StringBuilder(username.length());
        for (int i = 0; i < username.length(); i++) {
            char c = username.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                out.append(c);
            }
        }
        return out.length() == 0 ? null : out.toString();
    }
}
