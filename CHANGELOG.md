# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-11-06

### Added

- Initial release for Minecraft Forge 1.6.4.
- Modern 64x64 player skins with classic 4px and slim 3px arm models.
- Modern jacket, sleeve and trouser overlay layers with alpha blending.
- Cape rendering from Yggdrasil profiles, direct URL templates and local files.
- Third-party Yggdrasil / Blessing Skin support, including authlib-injector API
  discovery, name lookup, `textures` parsing, slim metadata and capes.
- Mojang profile fallback, direct URL templates and local skin/cape overrides.
- Local overrides under `config/vestiarium/`:
  `skins/<name>.png`, `skins/<name>.slim.png` and `capes/<name>.png`.
- Asynchronous profile resolution, image disk cache, HTTP timeouts and size
  limits.
- Retrolytra elytra cape compatibility.
- Build mod GitHub Actions workflow and project documentation.
- Requires Retrospectum Mixin 0.2.0.

### Changed

- Updated the template toolchain to Retrospectum Loom 0.2.1 and Retrospectum
  Mixin 0.2.0.
- Release JAR now uses the standard `MixinConfigs` manifest attribute.
- Generated `BuildConstants` replace source-token expansion for mod metadata.
- Setup records local JDK paths in `gradle.local.properties`.
- Relicensed under the GNU Lesser General Public License v3.0.

[Unreleased]: https://github.com/VulpesStella/Vestiarium/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/VulpesStella/Vestiarium/releases/tag/v0.1.0
