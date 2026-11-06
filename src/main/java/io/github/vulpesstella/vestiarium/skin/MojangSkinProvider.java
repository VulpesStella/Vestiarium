package io.github.vulpesstella.vestiarium.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.vulpesstella.vestiarium.Vestiarium;

/**
 * The official Mojang profile API. Modern skins and slim models are available
 * here when the player name belongs to a premium account.
 */
public final class MojangSkinProvider implements SkinProvider {
    private static final String LOOKUP_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    @Override
    public PlayerSkin resolve(String username) {
        try {
            String lookup = SkinHttp.getJson(LOOKUP_URL + SkinHttp.urlEncode(username));
            if (lookup == null || lookup.trim().length() == 0) {
                return null;
            }
            JsonObject object = new JsonParser().parse(lookup).getAsJsonObject();
            if (!object.has("id")) {
                return null;
            }
            String id = normalizeId(object.get("id").getAsString());
            if (id == null) {
                return null;
            }
            String profileUrl = PROFILE_URL + id;
            String profile = SkinHttp.getJson(profileUrl);
            PlayerSkin skin = ProfileTextureParser.parse(username, profile, profileUrl, false);
            if (skin != null) {
                Vestiarium.debug("Mojang skin resolved for " + username);
            }
            return skin;
        } catch (Throwable t) {
            Vestiarium.debug("Mojang lookup failed for " + username + ": " + t);
            return null;
        }
    }

    static String normalizeId(String value) {
        if (value == null) {
            return null;
        }
        String id = value.trim().replace("-", "").toLowerCase();
        if (id.length() != 32) {
            return null;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
                return null;
            }
        }
        return id;
    }
}
