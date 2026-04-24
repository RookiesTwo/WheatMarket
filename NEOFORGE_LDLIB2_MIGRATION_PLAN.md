# WheatMarket NeoForge + LDLib2 Migration Plan

## Goal

Migrate WheatMarket from the current Architectury multi-loader layout to a single NeoForge mod and replace the current custom Minecraft GUI implementation with LDLib2-based UI.

This is a migration plan, not an implementation patch. User requests are treated as goals; every concrete version, API, and file move should be verified against the current code, Gradle output, IDEA MCP metadata, and upstream docs before implementation.

## Current Project Facts

- Current build is Architectury multi-loader with `common`, `fabric`, and `neoforge` modules.
- Current Minecraft target is `1.21.1` with Java 21.
- Current NeoForge version is `21.1.138`.
- Architectury is used in build scripts, registries, events, client screen registration, networking, and environment detection.
- Existing custom UI is under `common/src/main/java/top/rookiestwo/wheatmarket/client/gui/**` plus `common/src/main/java/top/rookiestwo/wheatmarket/menu/**`.
- Existing GUI assets live under `common/src/main/resources/assets/wheatmarket/textures/gui/**`.
- Existing network code is Architectury `NetworkChannel` based.

## References Checked

- NeoForge 1.21.1 docs: Java 21 is required; build with `gradlew build`; run with generated run configs or `gradlew runClient` / `runServer`; server run requires EULA acceptance.
- NeoForge 1.21.1 mod files docs: `gradle.properties` owns common metadata; `neoforge.mods.toml` defines loader metadata, dependencies, mixins, and `@Mod` entrypoint linkage.
- NeoForge 1.21.1 registry docs: use NeoForge `DeferredRegister`, register it on the mod event bus from the mod constructor.
- NeoForge 1.21.1 events docs: mod constructor receives `IEventBus`; use the mod bus for lifecycle/registration events and `NeoForge.EVENT_BUS` for game events.
- NeoForge 1.21.1 networking docs: custom networking uses `RegisterPayloadHandlersEvent`, custom payloads, stream codecs, and handlers.
- LDLib2 docs: LDLib2 is a complete LDLib rewrite for modern Minecraft, with UI, shader, model rendering, data synchronization/persistence, and in-game editors.
- LDLib2 Java integration docs: for MC `1.21.1`, use maven `https://maven.firstdark.dev/snapshots` and dependency `com.lowdragmc.ldlib2:ldlib2-neoforge-${minecraft_version}:${ldlib2_version}:all`.
- LDLib2 maven metadata: latest available `ldlib2-neoforge-1.21.1` version is `2.2.6`.
- LDLib2 UI docs: choose UI type first; server data UIs must be menu-based with `ModularUI.of(ui, player)`; `BlockUIMenuType`, `HeldItemUIMenuType`, and `PlayerUIMenuType` handle common menu factories.
- LDLib2 XML docs: XML UI files can use `ldlib2-ui.xsd`, stylesheets, inline styles, and component trees; Java still loads XML and binds logic.

## Target Layout

Use a single NeoForge module at repository root:

```text
WheatMarket/
  build.gradle
  gradle.properties
  settings.gradle
  src/main/java/top/rookiestwo/wheatmarket/...
  src/main/resources/META-INF/neoforge.mods.toml
  src/main/resources/assets/wheatmarket/...
  src/main/resources/data/wheatmarket/...
```

Remove these after the root project builds:

- `common/`
- `fabric/`
- `neoforge/`
- `.architectury-transformer/`
- Architectury generated/intermediate build outputs

Keep package names as `top.rookiestwo.wheatmarket` unless there is a separate decision to rename the Java group/package.

## Phase 1: Convert Build System

Replace Architectury Loom + Architectury Plugin with a NeoForge single-loader build. For MC `1.21.1`, use Java 21.

Recommended baseline is NeoGradle because the checked LDLib2 `1.21` branch itself uses `net.neoforged.gradle.userdev` and because NeoForge 1.21.1 docs explicitly list NeoGradle as an MDK option.

`settings.gradle` target shape:

```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.5.0'
}

rootProject.name = 'WheatMarket'
```

`gradle.properties` target additions/renames:

```properties
mod_id=wheatmarket
mod_name=Wheat Market
mod_license=MIT
mod_authors=RookiesTwo
mod_description=Minecraft market mod for item trading and economy.
minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.22)
neo_version=21.1.219
neo_version_range=[21.1,)
loader_version_range=[2,)
ldlib2_version=2.2.6
```

Use `21.1.219` as the recommended NeoForge target because LDLib2 `2.2.6` upstream is built against `21.1.219`. If staying on `21.1.138`, verify runtime compatibility before coding UI against LDLib2 APIs.

`build.gradle` target concepts:

```gradle
plugins {
    id 'java-library'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.neoforged.gradle.userdev' version '7.0.181'
}

version = mod_version
group = maven_group

base {
    archivesName = archives_name
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven { url = 'https://maven.neoforged.net/releases' }
    maven { url = 'https://maven.firstdark.dev/snapshots' }
}

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
    implementation "com.lowdragmc.ldlib2:ldlib2-neoforge-${minecraft_version}:${ldlib2_version}:all"
    implementation "com.h2database:h2:2.3.232"
}

runs {
    client { }
    server { programArgument '--nogui' }
}
```

Decide H2 packaging during implementation:

- If H2 must be bundled inside WheatMarket, prefer NeoForge Jar-in-Jar or a clearly configured packaging mechanism.
- Do not bundle LDLib2. Treat it as a required frontend/library mod and declare it in `neoforge.mods.toml`.

## Phase 2: Move Sources and Resources

Move shared code/resources from `common` into root `src/main`:

- `common/src/main/java/top/rookiestwo/wheatmarket/**` -> `src/main/java/top/rookiestwo/wheatmarket/**`
- `common/src/main/resources/**` -> `src/main/resources/**`

Move NeoForge metadata and entrypoint into root:

- `neoforge/src/main/resources/META-INF/neoforge.mods.toml` -> `src/main/resources/META-INF/neoforge.mods.toml`
- Either move `neoforge/src/main/java/top/rookiestwo/wheatmarket/neoforge/WheatMarketNeoForge.java` into the root source tree or replace it with `@Mod(WheatMarket.MOD_ID)` on a NeoForge-specific bootstrap class.

Do not move Fabric-specific files. Delete Fabric entrypoints and metadata after the NeoForge build compiles.

## Phase 3: Replace Architectury APIs

Replace all `dev.architectury.*` imports. Current direct usages were found in commands, registry/bootstrap, database environment detection, and networking.

Migration map:

| Current Architectury usage | NeoForge replacement |
| --- | --- |
| `dev.architectury.registry.registries.DeferredRegister` | `net.neoforged.neoforge.registries.DeferredRegister` |
| `RegistrySupplier<T>` | `DeferredHolder<R, T>` or `Supplier<T>` depending on call site |
| `DeferredRegister.create(modId, registry)` style | `DeferredRegister.create(BuiltInRegistries.X, modId)` or specialized NeoForge helpers |
| `MenuRegistry.registerScreenFactory` | Client mod-bus `RegisterMenuScreensEvent#register` |
| `CommandRegistrationEvent` | `RegisterCommandsEvent` on `NeoForge.EVENT_BUS` |
| `LifecycleEvent.SERVER_STARTING` | NeoForge server lifecycle event, e.g. `ServerStartingEvent` |
| `LifecycleEvent.SERVER_STOPPING` | NeoForge server lifecycle event, e.g. `ServerStoppingEvent` |
| `LifecycleEvent.SERVER_LEVEL_SAVE` | NeoForge level/server save event; verify exact class in IDE for 1.21.1 |
| `PlayerEvent.PLAYER_JOIN` | `net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent` |
| `Platform.getEnvironment()` / `Env` | `FMLEnvironment.dist`, constructor `Dist`, or side-specific event subscribers |
| `NetworkChannel` / `PacketContext` | NeoForge payload registration via `RegisterPayloadHandlersEvent` and `CustomPacketPayload` |

Bootstrap target:

- The `@Mod` class constructor should receive `IEventBus modBus`.
- Register blocks, items, sounds, menus, and mod-bus listeners from that constructor.
- Register game-bus listeners on `NeoForge.EVENT_BUS`.
- Keep client-only registration isolated under a `client` package and subscribe only on `Dist.CLIENT`.

## Phase 4: Replace Networking

Current packet classes under `network/c2s` and `network/s2c` are Architectury `FriendlyByteBuf` + `PacketContext` style.

Target NeoForge design:

- Convert each packet to a `record` implementing `CustomPacketPayload`.
- Define a static `Type<T>` for each payload.
- Define a `StreamCodec<RegistryFriendlyByteBuf, T>` for serialization.
- Register payloads in `RegisterPayloadHandlersEvent`.
- Use the handler context to enqueue work and send responses.

Migration order:

1. Convert `OperationResultS2CPacket` and `BalanceUpdateS2CPacket` first because they are small and validate the S2C path.
2. Convert `RequestMarketListC2SPacket` and `MarketListS2CPacket` to prove market list sync.
3. Convert `BuyItemC2SPacket`, `ListItemC2SPacket`, and `ManageItemC2SPacket` after LDLib2 UI flow is selected.
4. Consider replacing some UI-specific requests with LDLib2 menu data binding or server-click handlers instead of keeping custom packets.

## Phase 5: Add LDLib2 as Required Frontend Mod

Add dependency to Gradle:

```gradle
repositories {
    maven { url = 'https://maven.firstdark.dev/snapshots' }
}

dependencies {
    implementation "com.lowdragmc.ldlib2:ldlib2-neoforge-${minecraft_version}:${ldlib2_version}:all"
}
```

Add required dependency to `neoforge.mods.toml`:

```toml
[[dependencies.wheatmarket]]
modId = "ldlib2"
type = "required"
versionRange = "[2.2.6,)"
ordering = "AFTER"
side = "BOTH"
```

Also remove the current required Architectury dependency block from `neoforge.mods.toml` after all Architectury imports and dependencies are gone.

Install the optional IDEA plugin `LDLib Dev Tool` if UI XML/LSS editing becomes heavy. LDLib2 docs say it supports highlighting, syntax checks, jump-to-definition, and completion.

## Phase 6: Deprecate Old UI and Introduce LDLib2 UI

Mark the following as legacy and stop adding features to them:

- `client/gui/WheatMarketMainScreen.java`
- `client/gui/MarketListingScreen.java`
- `client/gui/containers/**`
- `client/gui/widgets/**`
- `menu/WheatMarketMenu.java`
- `menu/MarketListingMenu.java`
- old GUI texture-only layout assets under `assets/wheatmarket/textures/gui/**` where no longer used

Do not delete the old UI in the same step as the build-system migration. First get a pure NeoForge project compiling, then switch one UI entrypoint at a time.

Recommended LDLib2 UI architecture:

- Use `BlockUIMenuType.BlockUI` for `LaptopBlock`, because the market opens from a placed block and needs server state.
- Return `ModularUI.of(UI.of(root), holder.player)` for server-synced UIs.
- Use `PlayerUIMenuType` later for command/keybind-opened market pages if needed.
- Put declarative layouts in `src/main/resources/assets/wheatmarket/ui/*.xml` where useful.
- Put shared LSS in `src/main/resources/assets/wheatmarket/lss/*.lss`.
- Use Java code to load XML via `XmlUtils.loadXml(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "ui/market_main.xml"))`, create `UI.of(xml)`, query elements, and bind data/actions.

Initial LDLib2 screen split:

1. `MarketMainUI`: list/search/filter page.
2. `MarketItemDetailUI`: detail and seller/admin management page.
3. `MarketPurchaseUI`: quantity selection, balance, total price, confirm.
4. `MarketListingUI`: player inventory, selected stack, price, flags, cooldown.

LDLib2 component mapping:

| Old UI concept | LDLib2 direction |
| --- | --- |
| custom `WheatButton` / `FilterButton` | `Button`, `Toggle`, `ToggleGroup`, or `Selector` |
| custom `WheatEditBox` / search bar | `TextField` plus `Button` or `SearchComponent` |
| paper placeholders | `ScrollerView` containing reusable item row/card `UIElement`s |
| player inventory display | `InventorySlots` or explicit `ItemSlot` bindings |
| item icon rendering | `ItemStackTexture` or `ItemSlot` depending on interaction needs |
| balance/status labels | `Label` with S2C/read-only data binding |
| confirm/buy/list actions | server-click handlers or retained NeoForge payloads |

Server state rule from LDLib2 docs:

- If a UI reads or writes server state, use a menu-based UI and pass `player` to `ModularUI.of`.
- Client-only `ModularUIScreen` is only for display-only overlays/tools and should not be used for market transactions.

## Phase 7: Economy and Data Safety During UI Migration

Keep all economic mutations server-side:

- balance changes
- stock changes
- listing creation/deletion
- cooldown checks
- purchase records

UI buttons should trigger server-side handlers. Do not trust client-provided price, stock, seller, or balance values.

When LDLib2 data binding replaces custom packets, still validate permissions and amounts on the server before writing to H2.

## Phase 8: Verification Plan

Use this order to reduce failures:

1. Build-system only migration: `./gradlew.bat build` should compile with no Fabric/Common modules.
2. Run `Minecraft Client (:neoforge)` or `./gradlew.bat runClient` after Gradle refresh.
3. Run `Minecraft Server (:neoforge)` or `./gradlew.bat runServer`; accept EULA in the run directory if needed.
4. Verify startup logs contain no Architectury dependency requirement.
5. Verify `neoforge.mods.toml` requires `ldlib2` and no longer requires `architectury`.
6. Open the laptop block and verify LDLib2 menu opens on a dedicated server, not only singleplayer.
7. Exercise market list, buy, list, manage, and cooldown flows with two players or fake accounts where possible.

Known environment issue from previous build attempt:

- Running Gradle with Java 8 fails before source compilation. Ensure `java -version` and Gradle toolchain resolve Java 21.

## Recommended Implementation Order

1. Add `.opencode/` to `.gitignore`.
2. Add the LDLib2 dependency and TOML dependency while still on current branch only if doing a small spike; otherwise wait until pure NeoForge build files exist.
3. Flatten source/resources into root `src/main`.
4. Replace build scripts with NeoForge-only build.
5. Replace registries/events/commands/environment APIs.
6. Replace networking APIs or intentionally delete packets made obsolete by LDLib2 UI handlers.
7. Build and run NeoForge client/server.
8. Implement `LaptopBlock` LDLib2 `BlockUIMenuType` entrypoint.
9. Build the minimal `MarketMainUI` with real server-synced data.
10. Port purchase/list/manage pages.
11. Delete old custom UI code and obsolete textures only after LDLib2 replacement reaches feature parity.

## Main Risks

- LDLib2 currently targets NeoForge `21.1.219` upstream while this project uses `21.1.138`; version alignment should be resolved early.
- Architectury networking migration is the largest API change and may affect every market operation.
- Old custom UI classes are intertwined with menu registration; deleting them before LDLib2 opens successfully will break the laptop interaction loop.
- H2 packaging must be preserved. If H2 is not bundled or otherwise available at runtime, database startup will fail.
- Client/server side separation becomes stricter after removing Architectury helpers. Keep all Minecraft client classes under `client` packages and register them only on the client side.
