package io.github.vulpesstella.vestiarium.mixin;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives RenderPlayerMixin a way to swap the protected main model for the
 * duration of one player render.
 */
@Mixin(RendererLivingEntity.class)
public interface RendererLivingEntityAccessor {
    @Accessor("mainModel")
    void vestiarium$setMainModel(ModelBase model);
}
