package io.github.vulpesstella.vestiarium.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.vulpesstella.vestiarium.Vestiarium;

import javax.xml.bind.DatatypeConverter;
import java.net.URL;

/**
 * Parses the {@code textures} property found in Mojang/blessing-skin style
 * profile responses. The property is a Base64 encoded JSON document.
 */
public final class ProfileTextureParser {
    private ProfileTextureParser() {
    }

    /**
     * @param username  display name used as the cache key
     * @param json      the profile response
     * @param profileUrl the profile endpoint; used to resolve relative texture URLs
     * @param local     whether the resulting URLs are local files
     */
    public static PlayerSkin parse(String username, String json, String profileUrl, boolean local) {
        if (json == null || json.trim().length() == 0) {
            return null;
        }
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray properties = root.has("properties") ? root.getAsJsonArray("properties") : null;
            if (properties == null) {
                return null;
            }

            String encodedTextures = null;
            for (JsonElement element : properties) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject property = element.getAsJsonObject();
                if (!property.has("name") || !property.has("value")) {
                    continue;
                }
                if ("textures".equals(property.get("name").getAsString())) {
                    encodedTextures = property.get("value").getAsString();
                    break;
                }
            }
            if (encodedTextures == null) {
                return null;
            }

            byte[] decoded = DatatypeConverter.parseBase64Binary(encodedTextures);
            String payloadJson = new String(decoded, "UTF-8");
            JsonObject payload = new JsonParser().parse(payloadJson).getAsJsonObject();
            if (!payload.has("textures")) {
                return null;
            }
            JsonObject textures = payload.getAsJsonObject("textures");

            String skinUrl = textureUrl(textures, "SKIN", profileUrl, local);
            String capeUrl = textureUrl(textures, "CAPE", profileUrl, local);
            SkinModel model = null;

            JsonObject skin = textureObject(textures, "SKIN");
            if (skin != null && skin.has("metadata") && skin.get("metadata").isJsonObject()) {
                JsonObject metadata = skin.getAsJsonObject("metadata");
                if (metadata.has("model")) {
                    String value = metadata.get("model").getAsString();
                    if ("slim".equalsIgnoreCase(value)) {
                        model = SkinModel.SLIM;
                    } else if ("default".equalsIgnoreCase(value) || "classic".equalsIgnoreCase(value)) {
                        model = SkinModel.CLASSIC;
                    }
                }
            }

            if (skinUrl == null && capeUrl == null) {
                return null;
            }
            return new PlayerSkin(username, skinUrl, capeUrl, model, local);
        } catch (Throwable t) {
            Vestiarium.debug("Could not parse profile for " + username + ": " + t);
            return null;
        }
    }

    private static JsonObject textureObject(JsonObject textures, String type) {
        JsonElement element = textures.get(type);
        if (element == null || !element.isJsonObject()) {
            element = textures.get(type.toLowerCase());
        }
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String textureUrl(JsonObject textures, String type, String profileUrl, boolean local) {
        JsonObject texture = textureObject(textures, type);
        if (texture == null || !texture.has("url")) {
            return null;
        }
        String url = texture.get("url").getAsString();
        if (url == null || url.length() == 0) {
            return null;
        }
        try {
            if (url.startsWith("//")) {
                url = "https:" + url;
            }
            URL parsed = new URL(url);
            String protocol = parsed.getProtocol();
            if (local) {
                if (!"file".equalsIgnoreCase(protocol)) {
                    return null;
                }
            } else if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return null;
            }
            return parsed.toExternalForm();
        } catch (Throwable t) {
            // Resolve a relative URL against the profile endpoint.
            try {
                URL base = new URL(profileUrl);
                URL resolved = new URL(base, url);
                String protocol = resolved.getProtocol();
                if (local ? "file".equalsIgnoreCase(protocol)
                        : ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))) {
                    return resolved.toExternalForm();
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }
}
