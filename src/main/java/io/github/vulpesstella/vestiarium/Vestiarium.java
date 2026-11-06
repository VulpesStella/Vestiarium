package io.github.vulpesstella.vestiarium;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import io.github.vulpesstella.vestiarium.config.VestiariumConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

@Mod(modid = Vestiarium.MOD_ID, name = Vestiarium.MOD_NAME, version = Vestiarium.VERSION)
public final class Vestiarium {
    // Generated from gradle.properties by the generateModConstants task.
    public static final String MOD_ID = BuildConstants.MOD_ID;
    public static final String MOD_NAME = BuildConstants.MOD_NAME;
    public static final String VERSION = BuildConstants.MOD_VERSION;

    @SidedProxy(
            clientSide = "io.github.vulpesstella.vestiarium.client.ClientProxy",
            serverSide = "io.github.vulpesstella.vestiarium.CommonProxy")
    public static CommonProxy proxy;

    private static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        VestiariumConfig.load(event);
        proxy.preInit(event);
        logger.info(MOD_NAME + " " + VERSION + " ready");
    }

    public static Logger logger() {
        if (logger == null) {
            logger = Logger.getLogger(MOD_ID);
        }
        return logger;
    }

    public static void debug(String message) {
        if (VestiariumConfig.debug()) {
            logger().log(Level.INFO, message);
        }
    }
}
