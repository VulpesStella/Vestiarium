package io.github.vulpesstella.vestiarium.skin;

import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.resources.ResourceManager;

/**
 * Placeholder used while a player's real skin is being resolved. The vanilla
 * steve texture is bound separately through AbstractClientPlayer.locationSkin;
 * this object only exists because RenderPlayer's deadmau5 check calls
 * getTextureSkin().isTextureUploaded().
 */
public final class PlaceholderTexture extends ThreadDownloadImageData {
    public static final PlaceholderTexture INSTANCE = new PlaceholderTexture();

    private PlaceholderTexture() {
        super("", null, null);
    }

    @Override
    public void loadTexture(ResourceManager resourceManager) {
    }

    @Override
    public boolean isTextureUploaded() {
        return true;
    }
}
