# Development

Setup records JDK selections in `.jdks/homes.properties` (used by the build
wrappers) and `gradle.local.properties` (used by Gradle/IDE import). Explicit
`-P` properties win. Run setup again after moving or removing a JDK.
`-Download` / `--download` forces project-local packages. `JAVA7_HOME`,
`JAVA8_HOME`, `JAVA17_HOME` and `JDK7`, `JDK8`, `JDK17` can supply existing
installations.

Only `io.github.vulpesstella.vestiarium.BuildConstants` is generated from mod
metadata; user Java sources are compiled in place. The generated package is
controlled by `modPackage` in `build.gradle`. Authors in `mcmod.info` come from
the comma-separated `mod_authors` property.

The mixins are client-only. Keep normal Minecraft method/field targets remapped;
the annotation processor writes an SRG refmap. Development launches remap it
back to MCP, and `remapJarForRelease` emits the SRG release mod. Do not hand-edit
generated refmaps.

Use `modImplementation`, `modCompileOnly` or `modRuntimeOnly` for other mods in
SRG distribution names; Loom remaps them for development. Plain libraries can
use standard `implementation` / `compileOnly` / `runtimeOnly` configurations.
Project dependencies such as `modImplementation project(':other-mod')` build
their producer archives before remapping. The producer must expose an SRG
distribution archive, as another Loom project does for its published Java
variants. Dependency remapping runs as Gradle tasks under
`build/retromixin/dependencies`; after a clean checkout, build once before
inspecting those generated JARs in an IDE.

Publishing is enabled in this project; `mod` uses `components.java`. Minecraft
and Forge development libraries are excluded from published variants.

For source development set `retrospectumLoomCheckout` to a Loom checkout. Set
`mixin_bootstrap` to a built shared runtime JAR, or
`retrospectumMixinCheckout` to a checkout whose built runtime matches
`mixin_version`. `retrospectumMavenUrl` overrides the hosted Maven repository for
both components.
