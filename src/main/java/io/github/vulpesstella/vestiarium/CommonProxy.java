package io.github.vulpesstella.vestiarium;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Server-side proxy. Vestiarium is a client mod, so this does no work on a
 * dedicated server.
 */
public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
    }
}
