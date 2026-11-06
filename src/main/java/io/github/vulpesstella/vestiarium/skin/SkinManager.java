package io.github.vulpesstella.vestiarium.skin;

import io.github.vulpesstella.vestiarium.Vestiarium;
import io.github.vulpesstella.vestiarium.config.VestiariumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolves skins on a worker pool and applies them on the client thread.
 * Player fields and TextureManager are only touched from the tick queue.
 */
public final class SkinManager {
    private static final Map<String, ResolvedSkin> CACHE =
            new ConcurrentHashMap<String, ResolvedSkin>();
    private static final ConcurrentHashMap<String, SkinModel> MODELS =
            new ConcurrentHashMap<String, SkinModel>();
    private static final Set<String> PENDING =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<Runnable>();
    private static final List<SkinProvider> PROVIDERS = new ArrayList<SkinProvider>();
    private static final LocalSkinProvider LOCAL_PROVIDER = new LocalSkinProvider();
    private static final ResourceLocation NO_CAPE = new ResourceLocation("vestiarium", "capes/none");

    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Vestiarium skin lookup " + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    });

    private static volatile boolean initialized;
    private static int tickCounter;

    private SkinManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        buildProviders();
        Vestiarium.debug("Skin providers: " + PROVIDERS.size() + ", source " + VestiariumConfig.source());
    }

    private static void buildProviders() {
        PROVIDERS.clear();
        VestiariumConfig.Source source = VestiariumConfig.source();
        if (source == VestiariumConfig.Source.OFF) {
            return;
        }
        if (source == VestiariumConfig.Source.LOCAL) {
            PROVIDERS.add(LOCAL_PROVIDER);
            return;
        }
        if (VestiariumConfig.localSkinsFirst()) {
            PROVIDERS.add(LOCAL_PROVIDER);
        }
        if (source == VestiariumConfig.Source.AUTO || source == VestiariumConfig.Source.YGGDRASIL) {
            for (String server : VestiariumConfig.yggdrasilServers()) {
                PROVIDERS.add(new YggdrasilSkinProvider(server));
            }
        }
        if (source == VestiariumConfig.Source.AUTO || source == VestiariumConfig.Source.MOJANG) {
            PROVIDERS.add(new MojangSkinProvider());
        }
        if (source == VestiariumConfig.Source.AUTO
                || source == VestiariumConfig.Source.YGGDRASIL
                || source == VestiariumConfig.Source.DIRECT) {
            PROVIDERS.add(new DirectSkinProvider());
        }
        if (!VestiariumConfig.localSkinsFirst()) {
            PROVIDERS.add(LOCAL_PROVIDER);
        }
    }

    /**
     * Called from the AbstractClientPlayer constructor. Installs the fallback
     * skin immediately and starts a background lookup if nothing is cached.
     */
    public static void request(AbstractClientPlayer player) {
        if (player == null) {
            return;
        }
        String username = player.getCommandSenderName();
        String key = key(username);
        PlayerSkinAccess access = access(player);
        if (access != null) {
            access.vestiarium$setSkin(AbstractClientPlayer.locationStevePng, PlaceholderTexture.INSTANCE,
                    NO_CAPE, NoCapeTexture.INSTANCE);
        }
        MODELS.putIfAbsent(key, SkinModel.LEGACY);
        if (!initialized) {
            return;
        }

        ResolvedSkin cached = CACHE.get(key);
        if (cached != null) {
            if (cached.skin != null) {
                apply(player, cached.skin);
            }
            if (!cached.isExpired()) {
                return;
            }
        }
        schedule(username, key);
    }

    private static void schedule(final String username, final String key) {
        if (!PENDING.add(key)) {
            return;
        }
        WORKERS.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final PlayerSkin skin = lookup(username);
                    long ttl = skin != null && skin.hasSkin()
                            ? VestiariumConfig.cacheMinutes()
                            : VestiariumConfig.retryMinutes();
                    CACHE.put(key, new ResolvedSkin(skin, System.currentTimeMillis() + ttl * 60L * 1000L));
                    if (skin != null && !skin.isEmpty()) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                applyToAll(username, skin);
                            }
                        });
                    }
                } catch (Throwable t) {
                    Vestiarium.logger().warning("Skin lookup failed for " + username + ": " + t);
                } finally {
                    PENDING.remove(key);
                }
            }
        });
    }

    private static void applyToAll(String username, PlayerSkin skin) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            return;
        }
        try {
            for (Object object : world.playerEntities) {
                if (object instanceof AbstractClientPlayer) {
                    AbstractClientPlayer player = (AbstractClientPlayer) object;
                    if (player.getCommandSenderName().equalsIgnoreCase(username)) {
                        apply(player, skin);
                    }
                }
            }
        } catch (Throwable t) {
            Vestiarium.logger().warning("Could not apply skin for " + username + ": " + t);
        }
    }

    private static void apply(AbstractClientPlayer player, PlayerSkin skin) {
        if (skin == null) {
            return;
        }
        String key = key(player.getCommandSenderName());
        PlayerSkinAccess access = access(player);
        if (access == null) {
            return;
        }
        Vestiarium.debug("Apply " + key + " skin=" + skin.skinUrl + " cape=" + skin.capeUrl
                + " modelHint=" + skin.model + " local=" + skin.local);

        ResourceLocation skinLocation = null;
        ThreadDownloadImageData skinTexture = null;
        if (skin.hasSkin()) {
            skinLocation = new ResourceLocation("vestiarium", "skins/" + key + "_" + shortHash(skin.skinUrl));
            final String modelKey = key;
            DownloadedTexture texture = new DownloadedTexture(
                    skin.skinUrl,
                    new SkinImageBuffer(key, skin.model),
                    DefaultTextures.skin(),
                    (skin.local || isFileUrl(skin.skinUrl)) ? null : TextureCache.file(skin.skinUrl, "skins"),
                    skin.local || isFileUrl(skin.skinUrl),
                    new DownloadedTexture.Listener() {
                        @Override
                        public void onFailed() {
                            onSkinFailed(modelKey);
                        }
                    });
            try {
                boolean loaded = Minecraft.getMinecraft().getTextureManager().loadTexture(skinLocation, texture);
                Vestiarium.debug("Register " + skinLocation + " ok=" + loaded);
                if (loaded) {
                    skinTexture = texture;
                } else {
                    skinLocation = null;
                }
            } catch (Throwable t) {
                Vestiarium.logger().warning("Could not register skin for " + key + ": " + t);
                skinLocation = null;
                skinTexture = null;
            }
        }
        if (skinLocation == null) {
            skinLocation = AbstractClientPlayer.locationStevePng;
            skinTexture = PlaceholderTexture.INSTANCE;
        }

        ResourceLocation capeLocation = NO_CAPE;
        ThreadDownloadImageData capeTexture = NoCapeTexture.INSTANCE;
        if (VestiariumConfig.enableCapes() && skin.hasCape()) {
            ResourceLocation candidate = new ResourceLocation("vestiarium", "capes/" + key + "_" + shortHash(skin.capeUrl));
            DownloadedTexture texture = new DownloadedTexture(
                    skin.capeUrl,
                    new CapeImageBuffer(),
                    null,
                    (skin.local || isFileUrl(skin.capeUrl)) ? null : TextureCache.file(skin.capeUrl, "capes"),
                    skin.local || isFileUrl(skin.capeUrl),
                    null);
            try {
                if (Minecraft.getMinecraft().getTextureManager().loadTexture(candidate, texture)) {
                    capeLocation = candidate;
                    capeTexture = texture;
                }
            } catch (Throwable t) {
                Vestiarium.debug("Could not register cape for " + key + ": " + t);
            }
        }

        access.vestiarium$setSkin(skinLocation, skinTexture, capeLocation, capeTexture);
        Vestiarium.debug("Applied " + key + " skinLocation=" + skinLocation + " capeLocation=" + capeLocation);
    }

    private static PlayerSkin lookup(String username) {
        PlayerSkin best = null;
        for (SkinProvider provider : PROVIDERS) {
            try {
                PlayerSkin skin = provider.resolve(username);
                if (skin == null || skin.isEmpty()) {
                    continue;
                }
                if (best == null) {
                    best = skin;
                } else {
                    best = merge(best, skin, username);
                }
                if (best.hasSkin()) {
                    break;
                }
            } catch (Throwable t) {
                Vestiarium.debug("Provider failed for " + username + ": " + t);
            }
        }
        if (best != null && best.hasSkin() && !best.hasCape() && VestiariumConfig.enableCapes()) {
            String cape = LocalSkinProvider.capeUrl(username);
            if (cape == null) {
                cape = DirectSkinProvider.capeUrl(username);
            }
            if (cape != null) {
                best = best.withCape(cape);
            }
        }
        return best;
    }

    private static PlayerSkin merge(PlayerSkin first, PlayerSkin second, String username) {
        String skinUrl = first.skinUrl != null ? first.skinUrl : second.skinUrl;
        String capeUrl = first.capeUrl != null ? first.capeUrl : second.capeUrl;
        SkinModel model = first.model != null ? first.model : second.model;
        boolean local = first.skinUrl != null ? first.local : second.local;
        return new PlayerSkin(username, skinUrl, capeUrl, model, local);
    }

    public static SkinModel getModel(String username) {
        SkinModel model = MODELS.get(key(username));
        if (model == null) {
            return SkinModel.LEGACY;
        }
        if (model == SkinModel.SLIM && !VestiariumConfig.enableSlim()) {
            return SkinModel.CLASSIC;
        }
        return model;
    }

    public static void onSkinParsed(String key, SkinModel model) {
        if (key != null && model != null) {
            MODELS.put(key, model);
        }
    }

    public static void onSkinFailed(String key) {
        if (key != null) {
            MODELS.put(key, SkinModel.LEGACY);
        }
    }

    public static void drainTasks() {
        Runnable task;
        int count = 0;
        while ((task = TASKS.poll()) != null && count++ < 64) {
            try {
                task.run();
            } catch (Throwable t) {
                Vestiarium.logger().warning("Skin task failed: " + t);
            }
        }
    }

    /**
     * Re-applies cached skins to players whose texture was not registered yet
     * (for example a player that was constructed before the lookup finished)
     * and repairs textures lost during a resource pack reload.
     */
    public static void tick() {
        if (!initialized) {
            return;
        }
        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            return;
        }
        try {
            for (Object object : world.playerEntities) {
                if (!(object instanceof AbstractClientPlayer)) {
                    continue;
                }
                AbstractClientPlayer player = (AbstractClientPlayer) object;
                ResolvedSkin cached = CACHE.get(key(player.getCommandSenderName()));
                if (cached == null || cached.skin == null) {
                    continue;
                }
                ResourceLocation current = player.getLocationSkin();
                boolean placeholder = current == null || current == AbstractClientPlayer.locationStevePng;
                boolean missing = current != null
                        && Minecraft.getMinecraft().getTextureManager().getTexture(current) == null;
                if (missing || (placeholder && cached.skin.hasSkin())) {
                    apply(player, cached.skin);
                }
            }
        } catch (Throwable t) {
            Vestiarium.debug("Tick skin application failed: " + t);
        }
    }

    private static String shortHash(String url) {
        String hash = TextureCache.sha1(url);
        return hash.length() > 10 ? hash.substring(0, 10) : hash;
    }

    private static boolean isFileUrl(String url) {
        return url != null && url.regionMatches(true, 0, "file:", 0, 5);
    }

    public static void post(Runnable task) {
        if (task != null) {
            TASKS.add(task);
        }
    }

    static String key(String username) {
        if (username == null) {
            return "";
        }
        return StringUtils.stripControlCodes(username).toLowerCase(Locale.ROOT);
    }

    private static PlayerSkinAccess access(AbstractClientPlayer player) {
        if (player instanceof PlayerSkinAccess) {
            return (PlayerSkinAccess) player;
        }
        return null;
    }

    private static final class ResolvedSkin {
        final PlayerSkin skin;
        final long expiresAt;

        ResolvedSkin(PlayerSkin skin, long expiresAt) {
            this.skin = skin;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
