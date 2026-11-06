package io.github.vulpesstella.vestiarium.render;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

/**
 * A 64x32 armour model with slim arm geometry. Armour textures in 1.6.4 are
 * still 64x32; only the arm width differs from the vanilla model.
 */
public final class SlimArmorModel extends ModelBiped {
    public SlimArmorModel(float inflation) {
        super(inflation, 0.0F, 64, 32);

        this.bipedRightArm = new ModelRenderer(this, 40, 16);
        this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 3, inflation);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

        this.bipedLeftArm = new ModelRenderer(this, 40, 16);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 3, inflation);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
    }
}
