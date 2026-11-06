package io.github.vulpesstella.vestiarium.client;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import io.github.vulpesstella.vestiarium.CommonProxy;
import io.github.vulpesstella.vestiarium.skin.SkinManager;

public final class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        SkinManager.init();
        TickRegistry.registerTickHandler(new ClientTickHandler(), Side.CLIENT);
    }
}
