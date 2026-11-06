package io.github.vulpesstella.vestiarium.skin;

import io.github.vulpesstella.vestiarium.config.VestiariumConfig;

/**
 * Simple URL template source. Useful for skin servers that expose direct PNG
 * URLs and for testing.
 */
public final class DirectSkinProvider implements SkinProvider {
    @Override
    public PlayerSkin resolve(String username) {
        String skin = skinUrl(username);
        String cape = capeUrl(username);
        if (skin == null && cape == null) {
            return null;
        }
        return new PlayerSkin(username, skin, cape, null, false);
    }

    public static String skinUrl(String username) {
        return expand(VestiariumConfig.directSkinUrl(), username);
    }

    public static String capeUrl(String username) {
        return expand(VestiariumConfig.directCapeUrl(), username);
    }

    private static String expand(String template, String username) {
        if (template == null || template.trim().length() == 0) {
            return null;
        }
        String encoded = SkinHttp.urlEncode(username);
        String expanded = template.trim()
                .replace("{username}", encoded)
                .replace("{name}", encoded);
        if (expanded.length() == 0) {
            return null;
        }
        String lower = expanded.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("file:")) {
            return null;
        }
        return expanded;
    }
}
