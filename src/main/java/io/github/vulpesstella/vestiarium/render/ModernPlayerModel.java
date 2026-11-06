package io.github.vulpesstella.vestiarium.render;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

/**
 * The 64x64 player model. It keeps ModelBiped's animation logic (sneaking,
 * riding, item use, bow aiming and armour passes all still write to the same
 * fields) but uses the modern texture layout and adds the jacket, sleeve and
 * trouser overlay boxes.
 */
public final class ModernPlayerModel extends ModelBiped {
    private final boolean slim;

    public ModernPlayerModel(boolean slim) {
        super(0.0F, 0.0F, 64, 64);
        this.slim = slim;
        this.boxList.clear();

        this.bipedHead = newPart(0, 0);
        this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.0F);
        this.bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.bipedHeadwear = newPart(32, 0);
        this.bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.5F);
        this.bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.bipedBody = newPart(16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.0F);
        this.bipedBody.setTextureOffset(16, 32);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.25F);
        this.bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);

        if (slim) {
            this.bipedRightArm = newPart(40, 16);
            this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 3, 0.0F);
            this.bipedRightArm.setTextureOffset(40, 32);
            this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 3, 0.25F);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

            this.bipedLeftArm = newPart(32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 3, 0.0F);
            this.bipedLeftArm.setTextureOffset(48, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 3, 0.25F);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        } else {
            this.bipedRightArm = newPart(40, 16);
            this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.0F);
            this.bipedRightArm.setTextureOffset(40, 32);
            this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.25F);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

            this.bipedLeftArm = newPart(32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.0F);
            this.bipedLeftArm.setTextureOffset(48, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.25F);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        }

        this.bipedRightLeg = newPart(0, 16);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
        this.bipedRightLeg.setTextureOffset(0, 32);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.25F);
        this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);

        this.bipedLeftLeg = newPart(16, 48);
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
        this.bipedLeftLeg.setTextureOffset(0, 48);
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.25F);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);

        this.bipedEars = newPart(24, 0);
        this.bipedEars.addBox(-3.0F, -6.0F, -1.0F, 6, 6, 1, 0.0F);

        // The cape is a separate 64x32 texture, not part of the skin atlas.
        this.bipedCloak = new ModelRenderer(this, 0, 0);
        this.bipedCloak.setTextureSize(64, 32);
        this.bipedCloak.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, 0.0F);
    }

    private ModelRenderer newPart(int u, int v) {
        ModelRenderer renderer = new ModelRenderer(this, u, v);
        renderer.setTextureSize(64, 64);
        return renderer;
    }

    public boolean isSlim() {
        return slim;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        // Modern skin overlays use alpha blending. Do not disturb the blend
        // state used for translucent (invisible) players.
        boolean blending = GL11.glIsEnabled(GL11.GL_BLEND);
        if (!blending) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        if (!blending) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }
}
