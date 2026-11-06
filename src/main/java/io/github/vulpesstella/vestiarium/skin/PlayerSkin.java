package io.github.vulpesstella.vestiarium.skin;

/**
 * A resolved skin and/or cape for one player name. Providers return these from
 * a background thread, so instances are immutable.
 */
public final class PlayerSkin {
    public final String username;
    public final String skinUrl;
    public final String capeUrl;
    /** May be null when the source does not know whether the skin is slim. */
    public final SkinModel model;
    /** True when the URLs point at files below config/vestiarium. */
    public final boolean local;

    public PlayerSkin(String username, String skinUrl, String capeUrl, SkinModel model, boolean local) {
        this.username = username;
        this.skinUrl = emptyToNull(skinUrl);
        this.capeUrl = emptyToNull(capeUrl);
        this.model = model;
        this.local = local;
    }

    public boolean hasSkin() {
        return skinUrl != null;
    }

    public boolean hasCape() {
        return capeUrl != null;
    }

    public boolean isEmpty() {
        return skinUrl == null && capeUrl == null;
    }

    public PlayerSkin withCape(String newCapeUrl) {
        if (newCapeUrl == null || newCapeUrl.length() == 0) {
            return this;
        }
        return new PlayerSkin(username, skinUrl, newCapeUrl, model, local);
    }

    public PlayerSkin withSkin(String newSkinUrl, SkinModel newModel, boolean newLocal) {
        if (newSkinUrl == null || newSkinUrl.length() == 0) {
            return this;
        }
        return new PlayerSkin(username, newSkinUrl, capeUrl, newModel, newLocal);
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
