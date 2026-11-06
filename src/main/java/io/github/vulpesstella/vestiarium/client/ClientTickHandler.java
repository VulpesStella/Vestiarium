package io.github.vulpesstella.vestiarium.client;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import io.github.vulpesstella.vestiarium.skin.SkinManager;

import java.util.EnumSet;

/**
 * Runs skin application tasks on the client thread. Texture downloads happen
 * off-thread, but TextureManager and player fields are only touched here.
 */
public final class ClientTickHandler implements ITickHandler {
    @Override
    public void tickStart(EnumSet<TickType> types, Object... tickData) {
    }

    @Override
    public void tickEnd(EnumSet<TickType> types, Object... tickData) {
        SkinManager.tick();
        SkinManager.drainTasks();
    }

    @Override
    public EnumSet<TickType> ticks() {
        return EnumSet.of(TickType.CLIENT);
    }

    @Override
    public String getLabel() {
        return "Vestiarium skin tasks";
    }
}
