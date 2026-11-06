package io.github.vulpesstella.vestiarium.mixin;

import io.github.vulpesstella.vestiarium.render.PlayerModels;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public abstract class RenderPlayerMixin {
    @Shadow
    private ModelBiped modelBipedMain;
    @Shadow
    private ModelBiped modelArmorChestplate;
    @Shadow
    private ModelBiped modelArmor;

    @Unique
    private ModelBiped vestiarium$armorChestplateBase;
    @Unique
    private ModelBiped vestiarium$armorBase;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void vestiarium$captureArmorModels(CallbackInfo ci) {
        this.vestiarium$armorChestplateBase = this.modelArmorChestplate;
        this.vestiarium$armorBase = this.modelArmor;
    }

    @Inject(method = "func_130009_a", at = @At("HEAD"))
    private void vestiarium$beginPlayerRender(AbstractClientPlayer player, double x, double y, double z,
                                              float yaw, float partialTicks, CallbackInfo ci) {
        ModelBiped model = PlayerModels.instance().forPlayer(player);
        this.modelBipedMain = model;
        ((RendererLivingEntityAccessor) (Object) this).vestiarium$setMainModel(model);

        // Select the armour model before vanilla copies heldItemRight/aimedBow/
        // isSneak onto it. setArmorModel itself runs later in the render pass.
        if (PlayerModels.instance().isSlim(player)) {
            this.modelArmorChestplate = PlayerModels.instance().armorChestplateSlim();
            this.modelArmor = PlayerModels.instance().armorSlim();
        } else {
            this.modelArmorChestplate = this.vestiarium$armorChestplateBase;
            this.modelArmor = this.vestiarium$armorBase;
        }
    }

    @Inject(method = "func_130009_a", at = @At("RETURN"))
    private void vestiarium$endPlayerRender(AbstractClientPlayer player, double x, double y, double z,
                                            float yaw, float partialTicks, CallbackInfo ci) {
        ModelBiped model = PlayerModels.instance().legacy();
        this.modelBipedMain = model;
        ((RendererLivingEntityAccessor) (Object) this).vestiarium$setMainModel(model);
    }

    @Inject(method = "renderFirstPersonArm", at = @At("HEAD"))
    private void vestiarium$beginFirstPersonArm(EntityPlayer player, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayer) {
            ModelBiped model = PlayerModels.instance().forPlayer((AbstractClientPlayer) player);
            this.modelBipedMain = model;
            ((RendererLivingEntityAccessor) (Object) this).vestiarium$setMainModel(model);
        }
    }

    @Inject(method = "renderFirstPersonArm", at = @At("RETURN"))
    private void vestiarium$endFirstPersonArm(EntityPlayer player, CallbackInfo ci) {
        ModelBiped model = PlayerModels.instance().legacy();
        this.modelBipedMain = model;
        ((RendererLivingEntityAccessor) (Object) this).vestiarium$setMainModel(model);
    }
}
