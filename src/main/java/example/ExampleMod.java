package example;

import cpw.mods.fml.common.Mod;

@Mod(modid = ExampleMod.MOD_ID, name = ExampleMod.MOD_NAME, version = ExampleMod.VERSION)
public final class ExampleMod {
    // Expanded from gradle.properties into build/prepared-sources/main/java.
    public static final String MOD_ID = "@MOD_ID@";
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final String VERSION = "@MOD_VERSION@";
}
