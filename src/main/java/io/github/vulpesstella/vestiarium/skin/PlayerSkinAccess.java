package io.github.vulpesstella.vestiarium.skin;

/**
 * Implemented by the AbstractClientPlayer mixin so the (private) vanilla skin
 * fields can be replaced without reflection.
 */
public interface PlayerSkinAccess {
    void vestiarium$setSkin(net.minecraft.util.ResourceLocation skin,
                            net.minecraft.client.renderer.ThreadDownloadImageData skinTexture,
                            net.minecraft.util.ResourceLocation cape,
                            net.minecraft.client.renderer.ThreadDownloadImageData capeTexture);
}
