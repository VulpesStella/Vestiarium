package io.github.vulpesstella.vestiarium.skin;

import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.resources.ResourceManager;

/**
 * A shared, never-uploaded placeholder used when a player has no cape. The
 * vanilla RenderPlayer asks for getTextureCape().isTextureUploaded() before it
 * binds a cape, so returning false cleanly disables cape rendering.
 */
public final class NoCapeTexture extends ThreadDownloadImageData {
    public static final NoCapeTexture INSTANCE = new NoCapeTexture();

    private NoCapeTexture() {
        super("", null, null);
    }

    @Override
    public void loadTexture(ResourceManager resourceManager) {
    }

    @Override
    public boolean isTextureUploaded() {
        return false;
    }
}
