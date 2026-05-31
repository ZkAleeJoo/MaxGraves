# MaxGraves

MaxGraves is a lightweight graves plugin for modern Minecraft servers. When a player dies, the plugin can store their items and experience in a temporary grave at or near the death location, give the player a locator map after respawn, and let them recover their contents safely.

## Features

- Creates a grave when a player dies and stores their dropped items and experience.
- Prevents the original death drops and dropped experience from duplicating after a grave is created.
- Gives the owner a locator map after respawn for every active grave that does not already have a locator in the player's inventory.
- Lets players right click their locator map to teleport to the linked grave when they have permission.
- Supports multiple active graves per player, sorted by remaining lifetime in the info menu.
- Opens a GUI with the player's active graves through `/maxgraves info`.
- Shows each grave's world, coordinates, remaining time, and internal grave ID in the info menu.
- Automatically equips recovered armor and off-hand items when the matching equipment slot is empty.
- Drops recovered overflow items at the player when their inventory is full.
- Stores and returns experience when the grave is claimed.
- Removes expired graves automatically after the configured lifetime.
- Removes the owner's locator when the linked grave is claimed or expires.
- Searches nearby blocks for a safe grave position when the exact death location is blocked.
- Supports world blacklists where graves should never be created.
- Respects `keepInventory`; no grave is created when the death event keeps the player's inventory.
- Supports private graves by default.
- Can optionally make graves from player kills public through `grave.access.public-player-kill`.
- Detects player killers, projectile killers, tamed entity owners, named mobs, and environmental deaths for hologram placeholders.
- Supports two grave marker modes: player head graves and chest graves.
- Protects grave blocks from normal breaking, explosions, fire, flowing blocks, pistons, and hopper-style item movement.
- Supports configurable holograms with placeholders.
- Supports configurable particles and ambient sounds for head graves.
- Supports configurable claim animations with visual lightning and sound.
- Supports bilingual message files through `general.language` (`en` and `es` are bundled).
- Automatically adds missing keys from bundled YAML files to existing config and language files.
- Supports runtime reload of settings, messages, grave visuals, update checks, and bStats state.
- Includes optional update checks and admin join notifications when a new version is detected.
- Includes optional anonymous bStats metrics.
- Includes death-event debug logging for troubleshooting drop or experience conflicts.

## Requirements

- Java 21.
- A modern Paper/Spigot-compatible server with API version `1.21`.
- Targets Paper API `26.1.2.build.53-stable`.

## Commands

The main command is `/maxgraves`.

Aliases:

- `/maxgrave`
- `/mg`
- `/graves`

| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| `/maxgraves` | None | Everyone | Shows the command usage message. |
| `/maxgraves info` | `maxgrave.info` | Everyone | Opens a menu with the player's active graves. Player-only command. |
| `/maxgraves reload` | `maxgrave.admin` | Operators | Reloads configuration, messages, grave settings, update checks, and bStats state. |

All aliases support the same subcommands, for example `/mg info` and `/mg reload`.

## Permissions

| Permission | Default | Used for |
| --- | --- | --- |
| `maxgrave.admin` | `op` | Allows `/maxgraves reload`, receives update notifications on join, and inherits `maxgrave.tp` and `maxgrave.info`. |
| `maxgrave.info` | `true` | Allows `/maxgraves info`. |
| `maxgrave.tp` | `false` | Allows the owner to teleport to their grave by right clicking a MaxGrave locator map. |

Grave claiming is not controlled by a Bukkit permission. It is controlled by ownership and the configured access policy:

- Owners can claim or open their own graves.
- Non-owners cannot claim private graves.
- If `grave.access.public-player-kill` is enabled, graves created by player kills can be opened or looted by other players.
- Locator maps can only be used by the owner of the linked grave, even when teleport permission is granted.

## Grave Marker Modes

### Head Mode

`grave.marker.type: "HEAD"`

Head graves place a player head marker. This mode supports:

- Holograms.
- Particle effects.
- Ambient grave sound.
- One-click claiming by right clicking or breaking the grave marker.
- Claim animation before rewards are returned.

### Chest Mode

`grave.marker.type: "CHEST"`

Chest graves place a chest marker and open a grave inventory. This mode supports:

- Holograms.
- Taking items one by one from the grave inventory.
- Automatic grave completion when the chest inventory becomes empty.
- Experience reward for the player who empties the grave.
- Overflow handling: if more than 54 item stacks would be stored, extra stacks are dropped at the grave location.

Chest mode intentionally does not use particles or claim animations.

## Configuration

Main settings live in `plugins/MaxGraves/config.yml`.

| Path | Default | Description |
| --- | --- | --- |
| `general.language` | `en` | Selects the language file. Bundled options are `en` and `es`. |
| `general.update-check` | `true` | Enables startup and periodic update checks. |
| `general.bstats` | `true` | Enables anonymous bStats metrics. |
| `general.prefix` | `MaxGraves` prefix | Prefix used in plugin messages. |
| `grave.despawn-time` | `3600` | Seconds before a grave expires. |
| `grave.blacklisted-worlds` | Example PvP worlds | Worlds where graves will not be created. |
| `grave.search-max-radius` | `6` | Maximum horizontal radius used to find a safe grave location. |
| `grave.debug-death-events` | `false` | Logs detailed death-event traces for troubleshooting. |
| `grave.create-on-death` | `true` | Enables or disables automatic grave creation on death. |
| `grave.marker.type` | `HEAD` | Grave marker mode. Available values: `HEAD`, `CHEST`. |
| `grave.access.public-player-kill` | `false` | Allows public access to graves created by player kills. |
| `grave.info-menu.item.material` | `PAPER` | Material used for each entry in the info menu. |
| `grave.hologram.enabled` | `true` | Enables grave holograms. |
| `grave.hologram.update-interval-ticks` | `20` | How often hologram text refreshes. |
| `grave.hologram.base-height` | `0.3` | Height offset from the marker to the first hologram line. |
| `grave.hologram.line-spacing` | `0.25` | Spacing between hologram lines. |
| `grave.hologram.lines` | Configured list | Lines shown above graves. |
| `grave.claim-animation.enabled` | `true` | Enables the claim animation for head graves. |
| `grave.claim-animation.delay-ticks` | `20` | Delay before rewards are returned after claiming. |
| `grave.claim-animation.lightning.enabled` | `true` | Plays visual lightning during the claim animation. |
| `grave.claim-animation.sound.type` | `ITEM_TOTEM_USE` | Sound played when claiming. |
| `grave.claim-animation.sound.volume` | `1.0` | Claim sound volume. |
| `grave.claim-animation.sound.pitch` | `0.75` | Claim sound pitch. |
| `grave.effects.enabled` | `true` | Enables particle effects for head graves. |
| `grave.effects.update-interval-ticks` | `5` | Particle update interval. |
| `grave.effects.primary-particle` | `SOUL` | Primary spiral particle. |
| `grave.effects.secondary-particle` | `SMOKE` | Secondary ambient particle. |
| `grave.effects.primary-count` | `4` | Primary particle amount per tick. |
| `grave.effects.secondary-count` | `2` | Secondary particle amount per tick. |
| `grave.effects.spiral-radius` | `0.7` | Spiral radius. |
| `grave.effects.spiral-height` | `1.3` | Spiral height. |
| `grave.effects.vertical-speed` | `0.05` | Vertical particle movement speed. |
| `grave.effects.ambient-sound.enabled` | `true` | Enables random ambient sounds near head graves. |
| `grave.effects.ambient-sound.type` | `BLOCK_SOUL_SAND_HIT` | Ambient sound type. |
| `grave.effects.ambient-sound.volume` | `0.45` | Ambient sound volume. |
| `grave.effects.ambient-sound.pitch` | `0.7` | Ambient sound pitch. |

## Hologram Placeholders

The following placeholders can be used in `grave.hologram.lines`:

| Placeholder | Description |
| --- | --- |
| `{player}` | Grave owner's name. |
| `{player_level}` | Grave owner's level at death. |
| `{killer}` | Killer name or configured unknown-killer text. |
| `{time_left}` | Remaining time before the grave expires. |
| `{x}` | Grave X coordinate. |
| `{y}` | Grave Y coordinate. |
| `{z}` | Grave Z coordinate. |

## Info Menu Placeholders

The following placeholders can be used in `messages.info-menu-item-name` and `messages.info-menu-item-lore`:

| Placeholder | Description |
| --- | --- |
| `{index}` | Grave number in the menu. |
| `{world}` | Grave world name. |
| `{x}` | Grave X coordinate. |
| `{y}` | Grave Y coordinate. |
| `{z}` | Grave Z coordinate. |
| `{time_left}` | Remaining time before the grave expires. |
| `{id}` | Internal grave UUID. |

## Messages

Message files are stored in:

- `plugins/MaxGraves/lang/messages_en.yml`
- `plugins/MaxGraves/lang/messages_es.yml`

The active file is selected with `general.language`.

The plugin supports legacy color codes and hex colors in messages.

## Runtime Notes

- Active graves are stored in memory while the server is running.
- Server shutdown or plugin disable removes active grave blocks and visuals.
- `/maxgraves reload` reloads settings without disabling the plugin.
- Existing config and language files are automatically updated with missing bundled keys.
- Invalid marker types fall back to `HEAD`.
- Invalid info menu materials fall back to `PAPER`.
- Invalid particles or sounds fall back to safe defaults and log a warning.

## Support

For questions, bugs, ideas, or suggestions, join the support Discord:

https://discord.gg/Vr46JHm2kd
