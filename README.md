# Vestiarium

Vestiarium brings modern Minecraft player skins to **Forge 1.6.4**: 64x64 skins,
the slim (Alex) player model, modern overlay layers, capes, and optional
third-party Yggdrasil skin servers such as Blessing Skin / LittleSkin.

The project started from the
[Retrospectum Mod Template](https://github.com/DesideriumMC/RetrospectumModTemplate)
and targets Minecraft Forge 1.6.4-9.11.1.1345 with
[Retrospectum Loom](https://github.com/DesideriumMC/RetrospectumLoom) and
[Retrospectum Mixin](https://github.com/DesideriumMC/RetrospectumMixin).

## Features

- 64x64 modern skins, both **classic** (4px arms) and **slim** (3px arms).
- Modern **jacket, sleeve and trouser overlay** layers, blended with alpha.
- Legacy 64x32 skins and the original player model still work unchanged.
- **Capes** from Yggdrasil profiles, direct URL templates or local files. The
  same resolved cape is also used by
  [Retrolytra](https://github.com/VulpesStella/Retrolytra)'s 1.6.4 elytra.
- **Third-party Yggdrasil / Blessing Skin support**, including
  authlib-injector API discovery, name lookup, the `textures` profile property,
  `model: slim` metadata and capes.
- Official Mojang profile fallback for premium names.
- Local overrides under `config/vestiarium/` for offline players and testing.
- Background profile resolution, disk cache for images, timeouts, and no
  server-side component.

## Requirements

| Purpose | Version |
| --- | --- |
| Minecraft / Forge | 1.6.4 / 9.11.1.1345 |
| Game JVM | Java 7 |
| Build JVM | JDK 8 for compilation, JDK 17 for Gradle (see [Development](docs/DEVELOPMENT.md)) |
| Runtime companion | Retrospectum Mixin 0.2.0 |

Vestiarium is client-side. It can be installed on a server without crashing,
but it does nothing there.

## Build

```powershell
./setup.ps1
./build.ps1 build genSources genIdeaRuns
```

or on Linux:

```sh
./setup.sh
./build.sh build genSources genIdeaRuns
```

The release JAR is written to `build/libs/vestiarium-retrospectum-1.6.4-<version>.jar`.
Install it together with Retrospectum Mixin **0.2.0**; the release uses the
standard `MixinConfigs` manifest attribute.

## Automated builds

The **Build mod** GitHub Actions workflow (`.github/workflows/build.yml`) builds
the project on pushes and pull requests. Download the `mod-<commit SHA>`
artifact to get the distributable SRG JAR; development `-dev.jar` archives are
excluded. Install Retrospectum Mixin separately alongside the mod.

## Configuration

The mod creates `config/vestiarium.cfg` on first launch. The important options:

```properties
general {
    S:skinSource=auto
    S:yggdrasilServers=
    S:directSkinUrl=
    S:directCapeUrl=
    B:enableCapes=true
    B:enableSlim=true
    B:localSkinsFirst=true
}

debug {
    B:debugLogging=false
}
```

### `skinSource`

| Value | Behaviour |
| --- | --- |
| `auto` | Local files first, then configured Yggdrasil servers, Mojang, then direct URLs. |
| `mojang` | Official Mojang profile API only. |
| `yggdrasil` | Configured Yggdrasil servers, then direct URLs. |
| `direct` | Only the `directSkinUrl` / `directCapeUrl` templates. |
| `local` | Only `config/vestiarium/skins` and `config/vestiarium/capes`. |
| `off` | No source; the vanilla Steve fallback is used. |

### Third-party Yggdrasil skin servers

Set the API root (or the skin site home page) in `yggdrasilServers`:

```properties
S:skinSource=yggdrasil
S:yggdrasilServers=https://littleskin.cn
```

`https://littleskin.cn/api/yggdrasil` also works. Multiple servers are comma or
newline separated and are tried in order.

For each player name Vestiarium:

1. Reads `X-Authlib-Injector-API-Location` from the configured URL when present,
   so a normal skin-site home page can be used.
2. Resolves the name with `GET {root}/api/users/profiles/minecraft/{name}`
   (authlib-injector's Mojang-compatible endpoint), falling back to
   `POST {root}/api/profiles/minecraft`.
3. Fetches `GET {root}/sessionserver/session/minecraft/profile/{uuid}`.
4. Decodes the Base64 `textures` property and reads `SKIN`, `CAPE` and
   `textures.SKIN.metadata.model` (`slim` / `default`).

Texture signatures are not verified; the URLs are treated as ordinary images.
Only `http` and `https` texture URLs are accepted from remote profiles.

### Local skins and capes

```
config/vestiarium/skins/<name>.png          classic / auto-detected model
config/vestiarium/skins/<name>.slim.png     force slim model
config/vestiarium/capes/<name>.png          cape (64x32, 22x17 or 64x64)
```

The name match is case-insensitive. A local skin takes priority when
`localSkinsFirst=true`; local capes are also used to fill in a cape when a
remote profile only supplied a skin.

### Direct URL templates

Useful for simple skin hosts:

```properties
S:directSkinUrl=https://example.com/skins/{username}.png
S:directCapeUrl=https://example.com/capes/{username}.png
```

`{username}` and `{name}` are replaced with the URL-encoded player name.

## How it works

- `AbstractClientPlayerMixin` replaces the dead 1.6.4 `skins.minecraft.net`
  lookup with Vestiarium's asynchronous resolver and its own texture objects.
- `RenderPlayerMixin` temporarily swaps in a modern `ModelBiped` while a player
  is rendered, then restores the vanilla model so other mods and the vanilla
  armour passes keep working.
- `ModernPlayerModel` uses the 64x64 UV layout, a 3px arm variant, and extra
  overlay boxes for the jacket, sleeves and trousers.
- `TextureManager` receives custom `ThreadDownloadImageData` subclasses that
  download (or read local/cached) images on a daemon thread and upload them on
  the render thread.
- Profiles are resolved on a two-thread pool and applied on the client tick, so
  the game thread never blocks on HTTP.

## Notes and limitations

- The 1.6.4 protocol does not carry profile textures. Skins are resolved by
  player name on the client. An offline player whose name also exists on Mojang
  can resolve to the Mojang skin depending on source order; use
  `skinSource=yggdrasil` or `local` to force a different source.
- A third-party site must implement a name-to-profile lookup like
  authlib-injector's `/api/users/profiles/minecraft/{name}`. Sites that only
  expose UUID endpoints need the direct URL templates or a local file.
- Capes are rendered by the vanilla 1.6.4 cape model and therefore expect a
  64x32 texture. 22x17 legacy capes are padded; 64x64 capes are cropped to the
  top half. 1.6.4 itself has no elytra; when
  [Retrolytra](https://github.com/VulpesStella/Retrolytra) is installed, the
  cape Vestiarium resolves is displayed on its elytra as well.
- Armour textures remain 64x32 in 1.6.4. Slim armour uses 3px arms but the
  armour texture layout is unchanged.
- Skins without `metadata.model` are classified by checking the pixels that
  only classic 4px arms use. A classic skin that leaves its arm backs fully
  transparent can be classified as slim; adding explicit `model: slim` metadata
  on the skin site is more reliable.
- HD skins (for example 128x128) are downscaled to 64x64 or 64x32 with nearest
  neighbour sampling.

## Additional documentation

- [Development, generated constants and dependency remapping](docs/DEVELOPMENT.md)
- [Compatibility and migration notes](docs/COMPATIBILITY.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)
- [Changelog](CHANGELOG.md)

## License

MIT. See [LICENSE](LICENSE).

---

## 中文快速说明

Vestiarium 为 Forge 1.6.4 带来现代 64x64 皮肤（支持 slim 细手臂）、现代外层
（外套/袖子/裤腿）、披风，以及第三方 Yggdrasil 皮肤站支持。

- 配置文件：`config/vestiarium.cfg`
- 第三方皮肤站示例：

  ```properties
  S:skinSource=yggdrasil
  S:yggdrasilServers=https://littleskin.cn
  ```

  也可以填写 `https://littleskin.cn/api/yggdrasil`。支持 authlib-injector 的
  `X-Authlib-Injector-API-Location`、按名称查询角色、`textures` 属性、slim
  元数据和披风。

- 与 [Retrolytra](https://github.com/VulpesStella/Retrolytra) 同时安装时，
  Vestiarium 解析出的披风也会显示在 Retrolytra 为 1.6.4 引入的鞘翅上。

- 本地皮肤：

  ```
  config/vestiarium/skins/玩家名.png
  config/vestiarium/skins/玩家名.slim.png
  config/vestiarium/capes/玩家名.png
  ```

- 编译：`./setup.ps1` 后执行 `./build.ps1 build`；产物在 `build/libs/`。
- 这是客户端模组，需要与 Retrospectum Mixin 一起安装。
