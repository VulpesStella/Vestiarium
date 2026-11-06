package io.github.vulpesstella.vestiarium.render;

import io.github.vulpesstella.vestiarium.skin.SkinManager;
import io.github.vulpesstella.vestiarium.skin.SkinModel;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;

/**
 * Shared model instances. Rendering is single threaded, so one instance per
 * layout is enough; the model's rotation fields are written on every render.
 */
public final class PlayerModels {
    private static volatile PlayerModels instance;

    private final ModelBiped legacy;
    private final ModernPlayerModel classic;
    private final ModernPlayerModel slim;
    private final ModelBiped armorChestplateSlim;
    private final ModelBiped armorSlim;

    private PlayerModels() {
        this.legacy = new ModelBiped(0.0F);
        this.classic = new ModernPlayerModel(false);
        this.slim = new ModernPlayerModel(true);
        this.armorChestplateSlim = new SlimArmorModel(1.0F);
        this.armorSlim = new SlimArmorModel(0.5F);
    }

    public static PlayerModels instance() {
        PlayerModels result = instance;
        if (result == null) {
            synchronized (PlayerModels.class) {
                result = instance;
                if (result == null) {
                    result = new PlayerModels();
                    instance = result;
                }
            }
        }
        return result;
    }

    public ModelBiped forPlayer(AbstractClientPlayer player) {
        SkinModel model = SkinManager.getModel(player.getCommandSenderName());
        if (model == SkinModel.SLIM) {
            return slim;
        }
        if (model == SkinModel.CLASSIC) {
            return classic;
        }
        return legacy;
    }

    public boolean isSlim(AbstractClientPlayer player) {
        return forPlayer(player) == slim;
    }

    public ModelBiped legacy() {
        return legacy;
    }

    public ModelBiped armorChestplateSlim() {
        return armorChestplateSlim;
    }

    public ModelBiped armorSlim() {
        return armorSlim;
    }
}
