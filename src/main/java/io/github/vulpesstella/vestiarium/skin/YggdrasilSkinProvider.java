package io.github.vulpesstella.vestiarium.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.vulpesstella.vestiarium.Vestiarium;

import java.net.URL;

/**
 * A generic Yggdrasil skin provider. It understands the authlib-injector
 * extensions used by Blessing Skin / LittleSkin and most other third party
 * skin sites:
 *
 * <ul>
 *   <li>{@code GET /api/users/profiles/minecraft/{name}} (Mojang compatible)</li>
 *   <li>{@code POST /api/profiles/minecraft} (batch name lookup)</li>
 *   <li>{@code GET /sessionserver/session/minecraft/profile/{uuid}}</li>
 *   <li>{@code X-Authlib-Injector-API-Location} response header discovery</li>
 * </ul>
 */
public final class YggdrasilSkinProvider implements SkinProvider {
    private final String configured;
    private volatile String apiRoot;
    private volatile boolean rootChecked;

    public YggdrasilSkinProvider(String configured) {
        this.configured = configured;
    }

    @Override
    public PlayerSkin resolve(String username) {
        try {
            String root = apiRoot();
            if (root == null) {
                return null;
            }
            String id = lookupId(root, username);
            if (id == null) {
                return null;
            }
            String profileUrl = root + "/sessionserver/session/minecraft/profile/" + id;
            String profile = SkinHttp.getJson(profileUrl);
            PlayerSkin skin = ProfileTextureParser.parse(username, profile, profileUrl, false);
            if (skin != null) {
                Vestiarium.debug("Yggdrasil skin resolved for " + username + " from " + root);
            }
            return skin;
        } catch (Throwable t) {
            Vestiarium.debug("Yggdrasil lookup failed for " + username + " at " + configured + ": " + t);
            return null;
        }
    }

    private String apiRoot() {
        if (rootChecked) {
            return apiRoot;
        }
        synchronized (this) {
            if (rootChecked) {
                return apiRoot;
            }
            rootChecked = true;
            apiRoot = discoverRoot(configured);
            return apiRoot;
        }
    }

    private static String discoverRoot(String configured) {
        String base = normalizeBase(configured);
        if (base == null) {
            return null;
        }
        String location = SkinHttp.getHeader(base, "X-Authlib-Injector-API-Location");
        if (location != null && location.trim().length() > 0) {
            try {
                URL resolved = new URL(new URL(ensureSlash(base)), location.trim());
                String candidate = trimSlash(resolved.toExternalForm());
                if (isHttp(candidate)) {
                    return candidate;
                }
            } catch (Throwable ignored) {
            }
        }
        if (base.endsWith("/api/yggdrasil")) {
            return base;
        }
        String candidate = base + "/api/yggdrasil";
        try {
            String metadata = SkinHttp.getJson(candidate);
            if (metadata != null && (metadata.indexOf("skinDomains") >= 0 || metadata.indexOf("\"meta\"") >= 0)) {
                return candidate;
            }
        } catch (Throwable ignored) {
        }
        return base;
    }

    private static String lookupId(String root, String username) {
        String id = null;
        try {
            String json = SkinHttp.getJson(root + "/api/users/profiles/minecraft/" + SkinHttp.urlEncode(username));
            id = parseId(json);
        } catch (Throwable ignored) {
        }
        if (id != null) {
            return id;
        }
        try {
            String body = "[\"" + SkinHttp.escapeJson(username) + "\"]";
            String json = SkinHttp.postJson(root + "/api/profiles/minecraft", body);
            if (json != null && json.trim().startsWith("[")) {
                JsonArray array = new JsonParser().parse(json).getAsJsonArray();
                for (JsonElement element : array) {
                    if (element.isJsonObject()) {
                        JsonObject object = element.getAsJsonObject();
                        if (object.has("id") && (!object.has("name")
                                || object.get("name").getAsString().equalsIgnoreCase(username))) {
                            return MojangSkinProvider.normalizeId(object.get("id").getAsString());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String parseId(String json) {
        if (json == null || json.trim().length() == 0 || !json.trim().startsWith("{")) {
            return null;
        }
        try {
            JsonObject object = new JsonParser().parse(json).getAsJsonObject();
            return object.has("id") ? MojangSkinProvider.normalizeId(object.get("id").getAsString()) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String normalizeBase(String value) {
        if (value == null) {
            return null;
        }
        String base = value.trim();
        if (base.length() == 0) {
            return null;
        }
        if (base.indexOf("://") < 0) {
            base = "https://" + base;
        }
        base = trimSlash(base);
        return isHttp(base) ? base : null;
    }

    private static String ensureSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }

    private static String trimSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean isHttp(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
