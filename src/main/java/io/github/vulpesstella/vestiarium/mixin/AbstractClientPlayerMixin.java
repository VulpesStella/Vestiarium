package io.github.vulpesstella.vestiarium.mixin;

import io.github.vulpesstella.vestiarium.skin.PlayerSkinAccess;
import io.github.vulpesstella.vestiarium.skin.SkinManager;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin implements PlayerSkinAccess {
    @Shadow
    private ResourceLocation locationSkin;
    @Shadow
    private ResourceLocation locationCape;
    @Shadow
    private ThreadDownloadImageData downloadImageSkin;
    @Shadow
    private ThreadDownloadImageData downloadImageCape;

    @Inject(method = "setupCustomSkin", at = @At("HEAD"), cancellable = true)
    private void vestiarium$setupCustomSkin(CallbackInfo ci) {
        SkinManager.request((AbstractClientPlayer) (Object) this);
        ci.cancel();
    }

    @Override
    public void vestiarium$setSkin(ResourceLocation skin, ThreadDownloadImageData skinTexture,
                                   ResourceLocation cape, ThreadDownloadImageData capeTexture) {
        this.locationSkin = skin;
        this.downloadImageSkin = skinTexture;
        this.locationCape = cape;
        this.downloadImageCape = capeTexture;
    }
}
