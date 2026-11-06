package io.github.vulpesstella.vestiarium.config;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for Vestiarium. The file lives in {@code config/vestiarium.cfg};
 * downloaded images are cached below {@code config/vestiarium/}.
 */
public final class VestiariumConfig {
    public enum Source {
        AUTO,
        MOJANG,
        YGGDRASIL,
        DIRECT,
        LOCAL,
        OFF;

        public static Source parse(String value) {
            if (value == null) {
                return AUTO;
            }
            String normalized = value.trim().toUpperCase();
            for (Source source : values()) {
                if (source.name().equals(normalized)) {
                    return source;
                }
            }
            return AUTO;
        }
    }

    private static final List<String> YGGDRASIL_SERVERS = new ArrayList<String>();

    private static Source source = Source.AUTO;
    private static String directSkinUrl = "";
    private static String directCapeUrl = "";
    private static boolean enableCapes = true;
    private static boolean enableSlim = true;
    private static boolean localSkinsFirst = true;
    private static int cacheMinutes = 60;
    private static int retryMinutes = 10;
    private static int cacheDays = 7;
    private static int connectTimeoutMs = 5000;
    private static int readTimeoutMs = 10000;
    private static int maxImageBytes = 4 * 1024 * 1024;
    private static boolean debug = false;

    private static File configDir = new File("config/vestiarium");

    private VestiariumConfig() {
    }

    public static void load(FMLPreInitializationEvent event) {
        File file = event.getSuggestedConfigurationFile();
        if (file.getParentFile() != null) {
            configDir = new File(file.getParentFile(), "vestiarium");
        }
        ensureDirectory(configDir);
        ensureDirectory(new File(configDir, "cache"));
        ensureDirectory(new File(configDir, "skins"));
        ensureDirectory(new File(configDir, "capes"));

        Configuration config = new Configuration(file);
        try {
            config.load();
            source = Source.parse(config.get("general", "skinSource", "auto",
                    "Where player skins come from: auto, mojang, yggdrasil, direct, local or off.\n"
                            + "auto tries the configured Yggdrasil servers, then Mojang, then the URL templates.\n"
                            + "local only reads config/vestiarium/skins and config/vestiarium/capes.").getString());

            String servers = config.get("general", "yggdrasilServers", "",
                    "Comma or newline separated Yggdrasil API roots, e.g.\n"
                            + "https://littleskin.cn/api/yggdrasil\n"
                            + "A skin site home page such as https://littleskin.cn also works when it advertises\n"
                            + "X-Authlib-Injector-API-Location.").getString();
            YGGDRASIL_SERVERS.clear();
            for (String server : servers.split("[,\\r\\n]+")) {
                String trimmed = server.trim();
                if (trimmed.length() > 0) {
                    YGGDRASIL_SERVERS.add(trimmed);
                }
            }

            directSkinUrl = config.get("general", "directSkinUrl", "",
                    "Template for direct skin downloads. Use {username} as the placeholder, e.g.\n"
                            + "https://example.com/skins/{username}.png\n"
                            + "Leave empty to disable the direct source.").getString();
            directCapeUrl = config.get("general", "directCapeUrl", "",
                    "Template for direct cape downloads. Use {username} as the placeholder, e.g.\n"
                            + "https://example.com/capes/{username}.png\n"
                            + "Leave empty to disable direct capes.").getString();

            enableCapes = config.get("general", "enableCapes", true,
                    "Render capes provided by Yggdrasil profiles, direct URLs or local files.").getBoolean(true);
            enableSlim = config.get("general", "enableSlim", true,
                    "Use the 3 pixel slim player arms when a profile or the skin texture says so.").getBoolean(true);
            localSkinsFirst = config.get("general", "localSkinsFirst", true,
                    "Check config/vestiarium/skins before querying skin servers.").getBoolean(true);

            cacheMinutes = config.get("cache", "profileCacheMinutes", 60,
                    "Minutes to cache a resolved profile in memory. Set to 0 to always refresh.").getInt(60);
            retryMinutes = config.get("cache", "failedLookupRetryMinutes", 10,
                    "Minutes before a failed profile lookup is retried.").getInt(10);
            cacheDays = config.get("cache", "imageCacheDays", 7,
                    "Days to keep downloaded skin and cape images on disk. Set to 0 to disable the disk cache.").getInt(7);

            connectTimeoutMs = config.get("network", "connectTimeoutMs", 5000,
                    "HTTP connect timeout in milliseconds.").getInt(5000);
            readTimeoutMs = config.get("network", "readTimeoutMs", 10000,
                    "HTTP read timeout in milliseconds.").getInt(10000);
            maxImageBytes = config.get("network", "maxImageBytes", 4 * 1024 * 1024,
                    "Maximum skin or cape download size in bytes.").getInt(4 * 1024 * 1024);
            debug = config.get("debug", "debugLogging", false,
                    "Log extra information about skin lookups and downloads.").getBoolean(false);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }

        if (connectTimeoutMs < 1000) {
            connectTimeoutMs = 1000;
        }
        if (readTimeoutMs < 1000) {
            readTimeoutMs = 1000;
        }
        if (maxImageBytes < 65536) {
            maxImageBytes = 65536;
        }
        if (cacheMinutes < 0) {
            cacheMinutes = 0;
        }
        if (retryMinutes < 1) {
            retryMinutes = 1;
        }
        if (cacheDays < 0) {
            cacheDays = 0;
        }
    }

    private static void ensureDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            // The mod still works without the cache; logging here would be too early.
            return;
        }
    }

    public static Source source() {
        return source;
    }

    public static List<String> yggdrasilServers() {
        return Collections.unmodifiableList(YGGDRASIL_SERVERS);
    }

    public static String directSkinUrl() {
        return directSkinUrl;
    }

    public static String directCapeUrl() {
        return directCapeUrl;
    }

    public static boolean enableCapes() {
        return enableCapes;
    }

    public static boolean enableSlim() {
        return enableSlim;
    }

    public static boolean localSkinsFirst() {
        return localSkinsFirst;
    }

    public static int cacheMinutes() {
        return cacheMinutes;
    }

    public static int retryMinutes() {
        return retryMinutes;
    }

    public static int cacheDays() {
        return cacheDays;
    }

    public static int connectTimeoutMs() {
        return connectTimeoutMs;
    }

    public static int readTimeoutMs() {
        return readTimeoutMs;
    }

    public static int maxImageBytes() {
        return maxImageBytes;
    }

    public static boolean debug() {
        return debug;
    }

    public static File configDir() {
        return configDir;
    }

    public static File cacheDir() {
        return new File(configDir, "cache");
    }

    public static File skinDir() {
        return new File(configDir, "skins");
    }

    public static File capeDir() {
        return new File(configDir, "capes");
    }
}
