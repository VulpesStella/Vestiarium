# Retrospectum Mod Template

Starter project for Minecraft Forge 1.6.4 / 9.11.1.1345 with
[Retrospectum Loom](https://github.com/DesideriumMC/RetrospectumLoom) and
[Retrospectum Mixin](https://github.com/DesideriumMC/RetrospectumMixin).

## Requirements

| Purpose | JDK |
| --- | --- |
| Gradle 8.14.6 | 17 |
| Compilation and Mixin annotation processing | 8 |
| Target API and default game runtime | 7 |

## Setup

1. Click **Use this template → Create a new repository**, then clone your new repository.
2. Run the setup script for your platform:

   ```powershell
   # Windows x64
   ./setup.ps1
   ./build.ps1 build genSources genIdeaRuns
   ```

   ```sh
   # Linux x86_64 (glibc)
   ./setup.sh
   ./build.sh build genSources genIdeaRuns
   ```

3. Edit `gradle.properties` to configure the mod metadata, and set the project name in `settings.gradle`.
4. Replace the example Java package and mixin classes with your own.

Setup reuses installed JDKs, including those downloaded by IntelliJ IDEA. Missing
JDKs are downloaded and verified against `gradle/jdks.lock.tsv`, then installed
under `.jdks/7`, `.jdks/8` and `.jdks/17`. The pinned packages are
[Azul Zulu Community Availability builds](https://docs.azul.com/core/install/metadata-api). Linux requires glibc, Bash, curl, tar, sha256sum
and standard GNU utilities. Windows uses PowerShell 5.1 or later.

`build.ps1` and `build.sh` run setup automatically on first use, select JDK 17
for Gradle and forward their arguments to the Gradle Wrapper. Use the same scripts
with `runClient` or `runServer` to start the game.

To download all three SDKs into the project, use `./setup.ps1 -Download`
or `./setup.sh --download`. Downloads and local configuration are excluded from Git.

Setup registers the selected SDKs in Gradle's automatic discovery directory,
`~/.jdks`, using Windows directory junctions or Linux symbolic links. SDK files
remain in their original location. The tracked `gradle.properties` contains shared
mod configuration and commented manual JDK overrides; setup leaves it unchanged.
Local SDK selections are recorded in `.jdks/homes.properties` and excluded from Git.

For IntelliJ IDEA, run setup before importing the project. Gradle's daemon JVM
criteria select JDK 17, and Gradle Toolchains discover JDK 7 and 8 automatically.
`JAVA7_HOME`, `JAVA8_HOME`, `JAVA17_HOME` and `JDK7`/`JDK8`/`JDK17` are supported
during setup. Run setup again after moving the project to refresh its SDK links.

Without setup, Loom can also resolve an installed Java 7 JDK through Gradle
Toolchains. `java7Home` and `org.gradle.java.installations.paths` remain optional manual
overrides. The daemon JVM version is set in `gradle/gradle-daemon-jvm.properties`.

Gradle resolves Loom and Mixin from `https://maven.desiderium.dev/releases/`,
along with the remaining build and game dependencies from their upstream repositories.

## Build tool versions

Versions are configured by `loom_version` and `mixin_version` in
`gradle.properties`. For local Maven testing, set `retrospectumMavenUrl` to a
repository directory URI. Set `mixin_bootstrap` to use a local runtime JAR.

To develop against Loom or Mixin source, set `retrospectumLoomCheckout` or
`retrospectumMixinCheckout` to the corresponding checkout. The Mixin source mode
uses its built Bootstrap matching `mixin_version`.

## Mod configuration

Configure the mod in `gradle.properties`:

```properties
mod_id=examplemod
mod_name=Example Mod
mod_group=example
version=1.0.0
mod_description=An example Retrospectum mod.
mod_authors=Your Name,Another Author
mod_url=
mod_credits=
```

The build fills `ExampleMod.MOD_ID`, `MOD_NAME` and `VERSION`, and expands
`mcmod.info`. Authors are a comma-separated list. Generated Java sources are
written to `build/prepared-sources/main/java`.

`src/main/resources/mod.mixins.json` becomes `<mod_id>.mixins.json`, with a
`<mod_id>.mixins.refmap.json` refmap. Loom adds the configuration to the manifest
and remaps the release JAR to SRG names. The output JAR is named
`<mod_id>-retrospectum-1.6.4-<version>.jar`.

The example injects into all world constructors and prints a message. Constructor
selection uses a wildcard with remapping disabled because client and server
constructor signatures differ, while constructor names remain unchanged.

## Distribution

Build output is in `build/libs/`. Distribute the SRG release JAR produced by
`remapJarForRelease`. Install Retrospectum Mixin alongside the mod.

## License

MIT. See [LICENSE](LICENSE).
