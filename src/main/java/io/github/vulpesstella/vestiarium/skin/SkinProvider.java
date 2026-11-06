package io.github.vulpesstella.vestiarium.skin;

/**
 * A source of skins. Implementations may perform blocking network calls and
 * are always invoked from SkinManager's worker pool.
 */
public interface SkinProvider {
    PlayerSkin resolve(String username);
}
