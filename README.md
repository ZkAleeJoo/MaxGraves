<div align="center">
  <h1>MaxGraves - Official Wiki</h1>
  <p>
    <img src="https://img.shields.io/badge/version-3.1.0-blue" alt="Version">
    <img src="https://img.shields.io/badge/Java-25+-red" alt="Java">
    <img src="https://img.shields.io/badge/Paper--Folia-1.21--26.2+-green" alt="Paper-Folia">
    <img src="https://img.shields.io/badge/bStats-Active-orange" alt="bStats">
    <img src="https://img.shields.io/badge/Modrinth-Available-brightgreen" alt="Modrinth">
    <img src="https://img.shields.io/badge/Languages-EN_|_ES-blue" alt="Languages">
  </p>
</div>

Welcome to the official **MaxGraves** Wiki!  
MaxGraves is an advanced, lightweight, and high-performance death grave and inventory recovery plugin engineered natively for modern Minecraft servers. Built for PaperMC and fully compatible with the multi-threaded regional architecture of **Folia**, MaxGraves safely retains players' dropped items and experience inside protected graves upon death, provides intuitive GUI management and navigation maps, and ensures seamless recovery without duplicate exploits or TPS lag.

---

## Table of Contents

1. [Main Features](#1-main-features)
2. [Installation Guide & Server Requirements](#2-installation-guide--server-requirements)
3. [Grave Lifecycle & Protection Pipeline](#3-grave-lifecycle--protection-pipeline)
4. [Grave Marker Modes (Head vs Chest)](#4-grave-marker-modes-head-vs-chest)
5. [Teleportation & Locator Map System](#5-teleportation--locator-map-system)
6. [Commands List](#6-commands-list)
7. [Permission Nodes & LuckPerms Guide](#7-permission-nodes--luckperms-guide)
8. [Configuration Guide (`config.yml`)](#8-configuration-guide-configyml)
9. [Holograms, Placeholders & Visual Effects](#9-holograms-placeholders--visual-effects)
10. [Interactive GUI Menus Guide (`/maxgraves info`)](#10-interactive-gui-menus-guide-maxgraves-info)
11. [Multi-Language Support (`messages_en.yml` & `messages_es.yml`)](#11-multi-language-support-messages_enyml--messages_esyml)
12. [Multi-Threading & Folia Architecture](#12-multi-threading--folia-architecture)
13. [Update Checker & bStats Metrics](#13-update-checker--bstats-metrics)
14. [Frequently Asked Questions (FAQ)](#14-frequently-asked-questions-faq)
15. [Support & Community](#15-support--community)

---

## 1. Main Features

* **Secure Death Drop Preservation:** Safely stores all dropped items (hotbar, inventory, off-hand, and equipped armor) and experience levels into a temporary grave block upon death.
* **Zero-Duplication Protection Engine:** Employs an internal `DeathInventoryReconciler` and `ProcessedDeathInventoryGuard` to prevent item duplication, drop conflicts, or ghost items under laggy server conditions.
* **Respects `keepInventory`:** Automatically skips grave creation if the world or event has `keepInventory` enabled, or if another plugin preserves inventory on death.
* **Two Visual Grave Marker Modes:**
  * **Player Head Mode (`HEAD`):** Spawns the victim's custom player skull with animated particle spirals, ambient sounds, dynamic holograms, and a visual lightning claim animation.
  * **Chest Mode (`CHEST`):** Places a physical container that opens a custom grave inventory GUI, allowing players to withdraw items individually or claim everything at once.
* **Fast Travel & PDC Locator Maps:** Automatically gives the player an Adventure/Bukkit locator map on respawn, bound to their grave UUID via Persistent Data Container (`PDC`). Supports right-click teleportation with configurable warmup and cooldown gates.
* **Interactive Paginated GUI (`/maxgraves info`):** Comprehensive GUI allowing players to view active graves, check remaining lifetime, inspect death details, get fresh locator maps, or teleport with a single click.
* **Multi-Grave & Single-Active Modes:** Choose between allowing players to have multiple active graves simultaneously or enforcing a strict single-active limit (`grave.limit.single-active: true`).
* **Intelligent Safe Placement Algorithm:** Searches outward up to `search-max-radius` blocks to locate solid, non-hazardous terrain if the player dies in lava, void, water, walls, or protected regions.
* **Dynamic Floating Holograms:** Displays customizable floating text above graves with live placeholders for player name, death level, killer, coordinates, and live countdown timer.
* **Full Multi-Threading & Folia Native:** Fully compatible with Paper and Folia's regional multi-threaded architecture (`runAtLocation` / `RegionizedServer`), eliminating main-thread freezes.
* **Bilingual Multi-Language Support:** Out-of-the-box support for English (`messages_en.yml`) and Spanish (`messages_es.yml`), with full support for modern 24-bit RGB Hex colors (`&#RRGGBB`) and legacy formatting (`&`).
* **Non-Blocking Asynchronous Update Checker:** Periodic and on-join update checks that alert operators when new builds are published on Modrinth.
* **Runtime Hot-Reload:** Refresh all configuration parameters, language files, grave visuals, and active timers dynamically via `/maxgraves reload`.

---

## 2. Installation Guide & Server Requirements

### Prerequisites

| Requirement | Supported Version | Details |
| :--- | :--- | :--- |
| **Java** | **Java 25+** | Strictly required by the plugin's compiler release and bytecode target. |
| **Server Software** | **Paper 1.21 – 26.2+** | Fully compatible with PaperMC, Purpur, and **Folia**. |
| **Spigot** | Partial | Compatible only if the runtime environment supplies modern Paper/Adventure APIs. |

### Step-by-Step Installation

1. Download the latest compiled `.jar` file from [Modrinth](https://modrinth.com/plugin/maxgraves) or GitHub Releases.
2. Place the file (e.g., `MaxGraves-3.1.0.jar`) into your server's `plugins/` directory.
3. Start or restart your server to generate the default configuration and language files.
4. Review and customize `plugins/MaxGraves/config.yml` and `lang/messages_en.yml` (or `messages_es.yml`) to fit your server's gameplay rules.
5. Apply any configuration modifications without restarting by executing `/maxgraves reload` (or `/mg reload`) in-game or via console.

> [!NOTE]
> MaxGraves operates entirely standalone and does not require external databases or heavy dependencies. Simply drop the `.jar` into your `plugins/` folder and it is ready for production.

---

## 3. Grave Lifecycle & Protection Pipeline

When a player dies, MaxGraves routes the event through a multi-stage verification pipeline to ensure safe placement and prevent item loss or duplication:

```
Player Death (PlayerDeathEvent)
        │
        ▼
[1] Is world in grave.blacklisted-worlds? ────────(Yes)──► Normal Death Drops (No Grave)
        │ (No)
        ▼
[2] Does keepInventory gamerule apply? ──────────(Yes)──► Keep Items (No Grave)
        │ (No)
        ▼
[3] Is single-active limit enabled & active? ─────(Yes)──► Drop Items Normally (Limit Reached)
        │ (No)
        ▼
[4] Locate Safe Spot (search-max-radius) ─────────(Failed)► Drop Items Normally (No Safe Spot)
        │ (Safe Location Found)
        ▼
[5] Reconcile & Clear Death Drops (Zero-Dupe Engine)
        │
        ▼
[6] Place Grave Marker (HEAD / CHEST) + PDC Metadata
        │
        ▼
[7] Deliver PDC-Bound Locator Map to Player on Respawn
        │
        ▼
[8] Spawn Dynamic Holograms & Ambient Visual FX
        │
        ▼
┌───────────────────────────┴───────────────────────────┐
▼                                                       ▼
Player Claims Grave                                  Despawn Timer Expires
• Auto-equips armor & off-hand                      • Unclaimed contents cleared
• Returns stored XP & items                         • Marker, holograms & tasks purged
• Triggers lightning & sound FX                     • Linked locator map invalidated
```

### Grave Protection Pipeline

1. **Environmental Immunity:** Active grave blocks cannot be destroyed by TNT or creeper explosions, fire spread, lava flow, water flow, or piston pushes.
2. **Access Control (Private vs Public):**
   * **Private Graves (Default):** Only the player who died can open or claim their grave. Other players cannot interact with or steal from it.
   * **Public Player-Kill Graves:** When `grave.access.public-player-kill` is set to `true`, any grave created from a PvP death becomes lootable by any player.
3. **Physical Claim Requirement:** Players must physically travel to the grave marker in the world to claim their belongings; GUI menus provide fast travel (if allowed) and coordinates, but will never allow remote looting.

---

## 4. Grave Marker Modes (Head vs Chest)

MaxGraves supports two distinct visual marker types configured via `grave.marker.type` in `config.yml`:

| Feature | Head Mode (`HEAD`) | Chest Mode (`CHEST`) |
| :--- | :--- | :--- |
| **Visual Block** | Victim's custom Player Skull block (`PLAYER_HEAD`) | Vanilla Chest block (`CHEST`) |
| **Interaction** | Right-click or break the skull to claim instantly | Right-click opens the custom grave chest inventory |
| **Claim Animation** | Harmless visual lightning strike + Totem sound effect | None (clean container interaction) |
| **Particle Effects** | Double spiral (Soul particles) + Ambient Smoke | Disabled for clean chest aesthetics |
| **Ambient Sounds** | Soul sand ambient whispers | None |
| **Looting Style** | Instant recovery: auto-equips armor and offhand slots | Selective withdrawal: take individual items or shift-click all |
| **Auto-Removal** | Disappears immediately once claimed | Disappears automatically once the chest inventory is empty |
| **Overflow Handling**| Drops overflow items at the player's feet if inventory full | Drops extra stacks on the ground if stored items exceed 54 |

---

## 5. Teleportation & Locator Map System

### PDC-Bound Locator Maps

When a player respawns, MaxGraves creates a customized locator map:
* **Persistent Data Container (PDC):** The item is tagged with the exact grave UUID and owner UUID. Only the rightful owner can activate the map.
* **Information Display:** Lore displays the target world, coordinates ($X, Y, Z$), and remaining time before expiration.
* **Automatic Cleanup:** When the linked grave is claimed or expires, the locator map is automatically removed from the player's inventory.

### Teleportation Gates & Safety

If teleportation is enabled (`maxgrave.tp` permission), players can right-click their locator map or click the teleport button inside `/maxgraves info`:

```yaml
grave:
  teleport:
    cooldown-seconds: 5    # Time a player must wait after grave creation before teleporting
    warmup-seconds: 5      # Stand-still duration before the teleport triggers
    cancel-on-move: true   # Cancels warmup if the player changes whole blocks
    cancel-on-damage: true # Cancels warmup if the player takes any damage
```

---

## 6. Commands List

All commands use the primary command label `/maxgraves` or its shorthand aliases (`/maxgrave`, `/mg`, `/graves`).

| Command | Aliases | Permission | Sender | Description |
| :--- | :--- | :--- | :--- | :--- |
| `/maxgraves` | `/mg`, `/maxgrave`, `/graves` | *None* (Everyone) | Player / Console | Displays the usage message and available subcommands. |
| `/maxgraves info` | `/mg info`, `/maxgrave info` | `maxgrave.info` | Player Only | Opens the interactive GUI displaying all active graves and death details. |
| `/maxgraves reload` | `/mg reload`, `/maxgrave reload`| `maxgrave.admin` | Player / Console | Reloads `config.yml`, language files, grave visuals, timers, and bStats state. |

> [!NOTE]
> Console execution is fully supported for `/maxgraves reload`. Players without permission cannot view subcommands or receive tab completions.

---

## 7. Permission Nodes & LuckPerms Guide

MaxGraves implements a clear, hierarchical permission structure designed to integrate seamlessly with **LuckPerms**, UltraPermissions, or any standard permission manager:

| Permission Node | Default | Description |
| :--- | :--- | :--- |
| `maxgrave.admin` | `op` | Grants access to `/maxgraves reload`, receives update alerts on join, and inherits `maxgrave.tp` and `maxgrave.info`. |
| `maxgrave.info` | `true` | Allows players to use `/maxgraves info` and access the grave management GUI. |
| `maxgrave.tp` | `false` | Allows the grave owner to teleport to their grave using the locator map or GUI button. |

### LuckPerms Quick Setup

* **Allow default players to open the info menu (already true by default):**
  ```bash
  /lp group default permission set maxgrave.info true
  ```
* **Allow VIP / donor players to teleport to their graves:**
  ```bash
  /lp group vip permission set maxgrave.tp true
  ```
* **Grant full administrative permissions to staff:**
  ```bash
  /lp group admin permission set maxgrave.admin true
  ```

---

## 8. Configuration Guide (`config.yml`)

Below is the complete reference for `plugins/MaxGraves/config.yml`:

```yaml
# ==============================================================================
#                               MAXGRAVES CONFIGURATION
# ==============================================================================

# Support Discord: https://discord.gg/Vr46JHm2kd
general:
  language: "en"                      # Active language file: "en" or "es"
  update-check: true                  # Check for updates on startup and every 5 hours
  bstats: true                        # Send anonymous usage metrics to bStats
  prefix: "&#8A2BE2&lMaxGraves &8» "  # Prefix prepended to all plugin messages

grave:
  despawn-time: 3600                  # Lifetime in seconds before grave disappears (3600 = 1 hour)
  blacklisted-worlds:                 # Worlds where graves will NEVER be created
    - "pvp_nether"
    - "pvp_the_end"
    - "evento_pvp"
  search-max-radius: 6                # Max horizontal radius to find safe ground if death spot is obstructed
  debug-death-events: false           # Detailed console traces for PlayerDeathEvent and drop states
  create-on-death: true               # Enable or disable automatic grave generation

  teleport:
    cooldown-seconds: 5               # Cooldown seconds before teleporting to a new grave (0 = disabled)
    warmup-seconds: 5                 # Stand-still warmup before teleport executes (0 = instant)
    cancel-on-move: true              # Cancel warmup if player changes block
    cancel-on-damage: true            # Cancel warmup if player takes damage

  locator-map:
    enabled: true                     # Deliver PDC locator map upon respawn

  limit:
    single-active: true               # Limit players to only 1 active grave at a time

  marker:
    type: "HEAD"                      # Visual marker block: "HEAD" or "CHEST"

  access:
    public-player-kill: false         # If true, graves created by player kills are lootable by anyone

  info-menu:
    size: 54                          # Inventory size for /maxgraves info (must be multiple of 9)
    grave-slots:                      # Slots allocated for active grave entries
      - 10
      - 11
      - 12
      - 13
      - 14
      - 15
      - 16
      - 19
      - 20
      - 21
      - 22
      - 23
      - 24
      - 25
      - 28
      - 29
      - 30
      - 31
      - 32
      - 33
      - 34
      - 37
      - 38
      - 39
      - 40
      - 41
      - 42
      - 43
    expiring-threshold-seconds: 300   # Seconds remaining before grave icon switches to expiring item (CLOCK)
    details:
      summary-slot: 13                # Slot for grave summary icon in detail view
    sounds:
      enabled: true                   # Enable UI click sound effects
      click:
        type: "UI_BUTTON_CLICK"
        volume: 0.6
        pitch: 1.2
    filler:
      enabled: true                   # Decorative background filler panes
      fill-grave-slots: false         # Fill empty grave slots with glass panes
      material: "BLACK_STAINED_GLASS_PANE"
      name: " "
      lore: []
    item:
      material: "PAPER"               # Fallback item material
      head-material: "PLAYER_HEAD"    # Icon for private head graves
      chest-material: "CHEST"         # Icon for private chest graves
      public-material: "ENDER_EYE"    # Icon for public player-kill graves
      expiring-material: "CLOCK"      # Icon when grave is close to despawning
    buttons:
      previous-page:
        enabled: true
        slot: 45
        material: "ARROW"
        name: "&#D6D6D6Previous &8({previous_page})"
        lore:
          - "&7Go back one page."
      next-page:
        enabled: true
        slot: 53
        material: "ARROW"
        name: "&#D6D6D6Next &8({next_page})"
        lore:
          - "&7View more graves."
      refresh:
        enabled: true
        slot: 49
        material: "AMETHYST_SHARD"
        name: "&#7CFFB2Refresh"
        lore:
          - "&7Update the menu."
      close:
        enabled: true
        slot: 50
        material: "BARRIER"
        name: "&#FF6B6BClose"
        lore:
          - "&7Close this menu."
      back:
        enabled: true
        slot: 45
        material: "ARROW"
        name: "&#D6D6D6Back"
        lore:
          - "&7Return to your active graves."
      teleport:
        enabled: true
        slot: 30
        material: "ENDER_PEARL"
        name: "&#64B5FFTeleport"
        lore:
          - "&7Travel to this grave location."
          - "&8Requires maxgrave.tp"
      locator:
        enabled: true
        slot: 32
        material: "FILLED_MAP"
        name: "&#FFD36ELocator map"
        lore:
          - "&7Receive a fresh map."
      details:
        enabled: true
        slot: -1
        material: "BOOK"
        name: "&#C9A7FFDetails"
        lore:
          - "&7Shift click a grave to open details."

  hologram:
    enabled: true                     # Spawn floating holograms above grave markers
    update-interval-ticks: 20         # Refresh rate in ticks (20 ticks = 1 second)
    base-height: 0.3                  # Vertical offset from marker block
    line-spacing: 0.25                # Distance between each hologram line
    lines:                            # Configurable lines with live placeholders
      - "&7{player} &c☠"
      - "&8• &fLvl: &7{player_level}"
      - "&8• &cKilled by &f{killer}"
      - "&e{time_left}"

  claim-animation:
    enabled: true                     # Enable visual claim sequence on head graves
    delay-ticks: 20                   # Delay before rewards are delivered (20 ticks = 1 second)
    lightning:
      enabled: true                   # Harmless aesthetic lightning strike (no fire/damage)
    sound:
      type: "ITEM_TOTEM_USE"          # Sound effect played on claim
      volume: 1.0
      pitch: 0.75

  effects:
    enabled: true                     # Enable ambient particle spirals and sounds
    update-interval-ticks: 5          # Ticks between particle rendering (5 ticks = 0.25s)
    primary-particle: "SOUL"          # Spiral particle (e.g., SOUL, WITCH, FLAME)
    secondary-particle: "SMOKE"       # Base ambient smoke particle
    primary-count: 4                  # Primary particles per tick
    secondary-count: 2                # Secondary particles per tick
    spiral-radius: 0.7                # Horizontal radius of particle spiral
    spiral-height: 1.3                # Vertical height of particle spiral
    vertical-speed: 0.05              # Vertical upward motion speed
    ambient-sound:
      enabled: true                   # Play periodic ambient whispers near graves
      type: "BLOCK_SOUL_SAND_HIT"     # Sound enum name
      volume: 0.45
      pitch: 0.7
```

---

## 9. Holograms, Placeholders & Visual Effects

### Hologram Placeholders

Holograms dynamically update while the grave is active using these placeholders:

| Placeholder | Context | Description |
| :--- | :--- | :--- |
| `{player}` | Holograms & Menus | Username of the grave owner. |
| `{player_level}` | Holograms | Experience level of the player at death. |
| `{killer}` | Holograms & Menus | Name of the killer (player, mob name, projectile source, or "Unknown"). |
| `{time_left}` | Holograms & Menus | Formatted remaining time before expiration (e.g., `45m 12s`). |
| `{x}` | Holograms & Menus | Grave X block coordinate. |
| `{y}` | Holograms & Menus | Grave Y block coordinate. |
| `{z}` | Holograms & Menus | Grave Z block coordinate. |

### Info Menu Placeholders

| Placeholder | Context | Description |
| :--- | :--- | :--- |
| `{index}` | Menu Titles & Item Lore | Sequential number of the grave in the list. |
| `{page}` | Menu Titles | Current active page index (starts at 1). |
| `{pages}` | Menu Titles | Total number of pages available. |
| `{graves}` | Menu Titles & Lore | Total number of active graves owned by the viewer. |
| `{world}` | Menu Item Lore | Name of the world where the grave is located. |
| `{items}` | Menu Item Lore | Total number of item stacks stored in the grave. |
| `{exp}` | Menu Item Lore | Total experience points stored in the grave. |
| `{type}` | Menu Item Lore | Marker mode used by the grave (`HEAD` or `CHEST`). |
| `{access}` | Menu Item Lore | Access permission state (`Private` or `Public`). |
| `{id}` | Menu Item Lore | Internal unique UUID of the grave. |

---

## 10. Interactive GUI Menus Guide (`/maxgraves info`)

The `/maxgraves info` command opens an intuitive, interactive GUI built for seamless navigation:

```
┌─────────────────────────────────────────────────────────┐
│ Active graves 1/1                                    [X]│
├─────────────────────────────────────────────────────────┤
│                                                         │
│    [☠]     [☠]     [☠]     [☠]     [☠]     [☠]     [☠]   │
│   Grave 1 Grave 2 Grave 3 Grave 4 Grave 5 Grave 6 Grave 7│
│                                                         │
│    [☠]     [☠]     [☠]     [☠]     [☠]     [☠]     [☠]   │
│                                                         │
├─────────────────────────────────────────────────────────┤
│ [◄] Prev             [✦] Refresh              Next [►] │
└─────────────────────────────────────────────────────────┘
```

### Main Menu Features

* **Dynamic State Icons:**
  * **Player Head:** Normal private head grave.
  * **Chest:** Normal private chest grave.
  * **Ender Eye:** Public player-kill grave.
  * **Clock:** Grave is near expiration (under `expiring-threshold-seconds`).
* **Mouse Interactions:**
  * **Left Click:** Teleport directly to the grave (requires `maxgrave.tp`).
  * **Right Click:** Receive a fresh PDC locator map in your inventory.
  * **Shift Click:** Open the detailed inspection sub-menu.

### Detail View Sub-Menu

Displays comprehensive statistics about the selected grave in slot 13, featuring dedicated buttons to trigger teleportation, obtain locator maps, or return to the main list.

---

## 11. Multi-Language Support (`messages_en.yml` & `messages_es.yml`)

MaxGraves provides full bilingual message files located in `plugins/MaxGraves/lang/`:
* `messages_en.yml` (English)
* `messages_es.yml` (Spanish)

### Language Configuration

To switch languages, update `general.language` in `config.yml`:
```yaml
general:
  language: "es" # Switch to Spanish messages
```

### Color & Formatting Engine

All messages, lore strings, and titles support:
* **24-Bit Hex RGB Colors:** Modern hex format `&#RRGGBB` (e.g., `&#8A2BE2`, `&#4CFFB2`).
* **Legacy Color Codes:** Traditional Minecraft formatting (`&a`, `&c`, `&e`, `&l`, `&o`).
* **Automatic Config Merger:** When updating MaxGraves, missing keys from newer versions are automatically injected into existing configuration files without erasing your customizations.

---

## 12. Multi-Threading & Folia Architecture

Traditional graves plugins interact with world blocks, inventories, and holograms directly on the main thread, causing severe lag spikes on busy servers.

MaxGraves is built with an asynchronous, multi-threaded design:
* **Native Folia Detection:** Automatically identifies Folia runtimes via `RegionizedServer`.
* **Regional Task Scheduling:** Schedules grave creation, hologram refreshes, and particle rendering through `SchedulerAdapter` (`FoliaSchedulerAdapter` vs `BukkitSchedulerAdapter`).
* **Concurrency Safety:** Entity removal and inventory restoration tasks run within the regional tick thread owning the chunk coordinates, eliminating concurrency exceptions and deadlock risks.

---

## 13. Update Checker & bStats Metrics

### Automatic Update Checker
* Automatically queries GitHub/Modrinth release endpoints on startup and every 5 hours asynchronously.
* When an update is detected, server administrators with `maxgrave.admin` receive clean notification banners upon joining.
* Toggleable at any time via `general.update-check` in `config.yml`.

### Anonymous bStats Metrics
* Integrates [bStats](https://bstats.org/) (Plugin ID: `31607`) to collect anonymous server environment data (Java version, server version, player counts).
* Allows the author to gauge performance and target optimizations for upcoming updates.
* Can be toggled on or off via `general.bstats` in `config.yml`.

---

## 14. Frequently Asked Questions (FAQ)

<details>
<summary><b>What happens if a player dies in the void or lava?</b></summary>
<br>
MaxGraves includes an intelligent safe-spot search algorithm. It scans outward up to <code>search-max-radius</code> (default: 6 blocks) to find a solid, safe block with air above it. If a player dies in the void, the grave will be placed safely above the void floor on the nearest solid ground.
</details>

<details>
<summary><b>Can players duplicate items by dying or claiming repeatedly?</b></summary>
<br>
No. MaxGraves implements a strict <code>DeathInventoryReconciler</code> that takes an exact snapshot of dropped items, verifies quantities slot-by-slot, and clears dropped items before writing to the grave's persistent storage.
</details>

<details>
<summary><b>Does MaxGraves interfere with the keepInventory gamerule?</b></summary>
<br>
No. If a world has <code>keepInventory</code> set to <code>true</code>, MaxGraves detects that inventory contents were preserved and safely skips grave generation.
</details>

<details>
<summary><b>Can other players steal my grave items?</b></summary>
<br>
By default, all graves are strictly private and bound to the owner's UUID. Only the owner can open or claim them. If you want PvP kills to be lootable by opponents, enable <code>grave.access.public-player-kill: true</code> in <code>config.yml</code>.
</details>

<details>
<summary><b>What happens if a player's inventory is full when claiming?</b></summary>
<br>
MaxGraves first automatically equips any matching armor and off-hand equipment slots if they are empty. Any remaining items that do not fit into the player's main inventory are dropped safely at the player's feet.
</details>

<details>
<summary><b>Does MaxGraves require an external database like MySQL?</b></summary>
<br>
No. MaxGraves stores active grave states in memory and binds persistent metadata to blocks and items using Bukkit's native <code>PersistentDataContainer</code> (PDC). No external databases or drivers are required.
</details>

<details>
<summary><b>Does running /maxgraves reload delete active graves?</b></summary>
<br>
No. Executing <code>/maxgraves reload</code> updates configuration values, language strings, particle settings, and schedulers without resetting or deleting any active graves in the world.
</details>

---

## 15. Support & Community

Have questions, need help configuring the plugin, or want to suggest a new feature?

* **Official Download (Modrinth):** [modrinth.com/plugin/maxgraves](https://modrinth.com/plugin/maxgraves)
* **Official Discord Support:** [Join our Discord Server](https://discord.gg/Vr46JHm2kd)
