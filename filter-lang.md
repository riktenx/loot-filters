# Loot Filters

This guide describes how to write loot filters, casually referred to as "rs2f".

## ⚠️ You do not need to understand any of this to use the plugin

This page is a _scripting reference guide_ for writing loot filters. It describes how filter syntax works and how to actually write a filter.
In practice, as a player, this is probably not something you need to do or think about, especially if you're not familiar with programming concepts.

Instead you should use one of the filters that others have written. If you just want to get started that way, check out [filterscape.xyz](https://filterscape.xyz).

## Overview

A loot filter is a basic script that contains the following
* Self-identifying metadata (filter name, description, etc.)
* Any number of **rules** - each being a pair of a condition and a display config - that determines how to display items
  on the ground.

For example, this is a simple loot filter:

```
meta {
  name = "riktenx/test";
}

// if anything is worth more than 10k, give it a special border
apply (value:>10_000) {
  borderColor = "#ff80ff";
}

// the filter keeps going until it hits a "rule" with matching conditions
rule (name:"anglerfish") {
  color = MAGENTA;
}

rule (name:"coins") {
  color = YELLOW; // and also, coins
  showLootbeam = true;
}
```

Scriptable filters allow us to exercise both a deep and far-reaching level of control over how to display ground items.

## Whitespace and comments

Single-line comments are supported, delimited by `//`, and can appear anywhere on a line, as demonstrated above. The
parser will ignore all text from the comment marker until the end of the line.

Block-style comments, e.g. `/* block */` are also supported. Block comments can be multiple lines long. A block comment starts
with `/*` and ends with `*/`. All text in between those two markers is ignored.

The script parser will ignore whitespace in the way you'd expect it to for any other language. For example, these two
matcher expressions are semantically equivalent:

```
// relaxed
rule ( value : > 100 ) {
  color = BLUE;
}

rule(value:>100){color=BLUE;} // compact
```

## The `meta` block

Metadata for a filter is given in a special metadata block. This should generally be placed at the top of the script
text, although the parser doesn't know the difference.

```
meta {
    name = "riktenx/demo";
    description = "demo filter";
}
```

#### `name`

The user-friendly name of the filter.

**Loot filters are uniquely identified by their names**:
* If you try to import a loot filter without a name, the plugin will prompt you to enter one.
* If you try to import a loot filter that shares a name with an existing one, the plugin will prompt you to confirm that
  you wish to override it.

#### `description`

User-friendly description of this filter.

## Rules

A loot filter is written as a list of rules, like so:

```
(rule|apply) (<conditions...>) {
    <display_property1> = <value>;
    <display_property2> = <value>;
    ...
}
... more rules
```

There are **two (2)** top-level types of blocks
* TERMINAL rules, expressed via `rule` keyword. The filter stops evaluating
  for an item when it matches one of these.
* NON-TERMINAL rules, expressed via the `apply` keyword. Applies any display
  settings to the item when it matches, but keeps evaluating the script.

Rules evaluate top-down for a given ground item, terminating on the first matched
`rule`.

### ⚠️ Nested rules are not supported

For example, you cannot do the following:

```
rule (a) {
  // 1
  rule (b) {
    // 2
  }
}
```

instead, you would have to do

```
apply (a) {
  // 1
}

rule (a && b) {
  // 2
}
```

### Example: terminal vs non-terminal

Consider the following filter:

```
apply (value:>100_000) {
  borderColor = BLUE;
  showLootbeam = true;
}

rule (name:"*godsword") {
  textColor = "#00ffff";
}

rule (name:"bandos*") {
  textColor = "#ffa500";
  showLootbeam = false;
}
```

As an exercise, think through how the following items would be displayed:
* ancestral robe top
* bandos tassets
* bandos godsword
* zamorak godsword

### Conditions

Conditions are expressed in the form

```
(<condition type>:<arguments>)
```

You can use logical operators to express compound conditions, such as

```
(name:"blue dragonhide" && !quantity:==1)
```

Logical operators should work as you'd expect from any other language:
* `!`, `&&`, and `||` work
* `&&` has higher operation order than `||`
* you can use parentheses to control operation order e.g. `(v1 || v2) && v3 && !(v4 || v5)`
* parenthesis can be nested to an arbitrary depth e.g. `(!(v1 || v2) && (v3 || v4) && v5`

The following conditions are supported.

For rules that take operators (denoted by `<op>`), you can use the following comparators:
* `==` (there's no `!=` but just use `!` on the rule itself)
* `>`
* `<`
* `>=`
* `<=`

| name        | syntax                               | example                         | description |
|-------------|--------------------------------------|---------------------------------|-------------|
| id          | `id:<n>`, `id:[<n0>, <n1>, ...]`     | `id:995`, `id:[995, 996]`       |             |
| ownership   | `ownership:<n>`                      | `ownership:1`                   |             |
| name        | `name:<s>`, `name:[<s0>, <s1>, ...]` | `name:["Coins"]`                |             |
| quantity    | `quantity:<op><n>`                   | `quantity:>1`, `quantity:==10`  |             |
| value       | `value:<op><n>`                      | `value:>500`, `value:<=500`     |             |
| gevalue     | `gevalue:<op><n>`                    | `gevalue:>500`, `gevalue:<=500` |             |
| havalue     | `havalue:<op><n>`                    | `havalue:>500`, `havalue:<=500` |             |
| tradeable   | `tradeable:<b>`                      | `tradeable:true`                |             |
| stackable   | `stackable:<b>`                      | `stackable:true`                |             |
| noted       | `noted:<b>`                          | `noted:true`                    |             |
| area        | `area:[x0,y0,z0,x1,y1,z1`            |                                 |             |
| accountType | `accountType:<n>`                    | `accountType:1`                 |             |

#### area `area:[x0,y0,z0,x1,y1,z1]`

Match against the in-world location of an item, expressed in coordinates. This allows you to write filter behavior
constrained to a specific monster, etc.

Area is expressed as a list of _exactly_ six (6) integers that represent the boundary coordinates of the desired
activation area:

```
[x0, y0, z0, x1, y1, z1]
```

There are several excellent web-based tools for finding map coordinates, such as https://mejrs.github.io/osrs.

For example, the coordinate pair `[2240, 4032, 0, 2303, 4095, 0]` describes the area for Vorkath.

#### name `name:"..."` or `name:["...", "...", <...>]`

Match against a name or list of names, case-insensitive. You can also match with a wildcard `*` on either or both sides
of the name ( e.g. `"* dragonhide"` matches all dragonhide colors).

As a special case, `name:"*"` will match against everything.

#### id `id:995` or `name:[1,2,3, <...>]`

Match based on an exact item ID or list of IDs.

#### quantity `quantity:>500`, `quantity:==500`, etc.

Match based on an item's quantity. Comparison operator can be >, <, >=, <=, or ==.

!= is NOT supported, use ! instead, e.g. `!quantity:==1`.

#### value `value:>500`

Match based on an item's highest value (GE vs. HA). Supports the same comparison operators used in `quantity`.

#### gevalue `gevalue:>100_000`

Match based on an item's GE value.

#### havalue `havalue:>40_000`

Match based on an item's high alchemy (HA) value.

#### tradeable `tradeable:true` or `tradeable:false`

Match based on whether an item is tradeable.

#### stackable `stackable:true` or `stackable:false`

Match based on whether an item is stackable (noted, coins, fishbait etc.).

#### noted `noted:true` or `noted:false`

Match based on whether an item is a note.

#### ownership (`ownership:1`, `ownership:OWNERSHIP_SELF`, etc.)

Match based on item ownership state:
1. `0` or `OWNERSHIP_NONE` - typically this is for world spawns or byproducts like ashes (1*)
1. `1` or `OWNERSHIP_SELF` - item dropped by you / monster killed by you
1. `2` or `OWNERSHIP_OTHER` - item dropped by someone else / someone else's monster kill (2*)
1. `3` or `OWNERSHIP_GROUP` - item dropped by someone / monster kill in your ironman group

(1*) Some people have reported that monster drops in CoX have this ownership value, regardless of who got the kill
credit for the monster.

(2*) Dawnbringer in ToB, when dropped by another player, appears to have a value of `2` / `OWNERSHIP_OTHER` but you can
still pick it up even if you're an ironman. This appears to be the only item in the game that works this way.

## Display settings

Display settings, expressed inside of curly braces after a rule's condition, control every aspect of how the plugin
handles an item on the ground.

Display settings control the following:
* whether to hide an item in the text overlay
* overlay text styles (font type, border, colors, etc.)
* lootbeam
* notifications
* ground tile highlight

For example:

```
rule (name:"coins" && quantity:>100_000) {
  textColor = YELLOW;
  borderColor = YELLOW;
  showLootbeam = true;
  lootbeamColor = "#ffffff";
}
```

The following table lists supported display settings:

| name                         | value type        | ordinal macros | description                                                                                                                                                                                                 |
|------------------------------|-------------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| hidden                       | boolean           |                | Whether this item is comprehensively hidden. Setting hidden to true disables EVERYTHING else - no lootbeam, no tile highlight, etc.                                                                         |
| hideOverlay                  | boolean           |                | Whether this item is hidden in the overlay. Disables the text overlay but allows other features to work.                                                                                                    |
| color, textColor             | color string      |                | Color for the display text of the item.                                                                                                                                                                     |
| backgroundColor              | color string      |                | Background color behind the display text.                                                                                                                                                                   |
| borderColor                  | color string      |                | Border color around the display text.                                                                                                                                                                       |
| showLootbeam, showLootBeam   | boolean           |                | Show an in-world lootbeam on the item's tile.                                                                                                                                                               |
| showValue                    | boolean           |                | Include an item's value in the text overlay. The highest value between GE and HA price is chosen.                                                                                                           |
| showDespawn                  | boolean           |                | Show a despawn timer next to the text overlay. The type of despawn timer is controlled by config.                                                                                                           |
| notify                       | boolean           |                | Fire a system notification when the matched item drops.                                                                                                                                                     |
| textAccent                   | enum              | `TEXTACCENT_*` | Text accent to use:<li>1 = text shadow (default)</li><li>2 = outline</li><li>3 = none</li><li>4 = bold shadow</li>                                                                                          |
| textAccentColor              | color string      |                | Color for the text accent. Defaults to solid black.                                                                                                                                                         |
| lootbeamColor, lootBeamColor | color string      |                | Color for the lootbeam. Defaults to the text color when unset.                                                                                                                                              |
| fontType                     | enum              | `FONTTYPE_*`   | Font used for the display:<li>1 = small (default)</li><li>2 = normal</li><li>3 = bold</li>                                                                                                                  |
| menuTextColor                | color string      |                | Color for the menu entry text. Defaults to the text color when unset.                                                                                                                                       | 
| highlightTile                | boolean           |                | Whether to highlight the tile the item is on.                                                                                                                                                               | 
| tileStrokeColor              | color string      |                | Color for tile outline. Defaults to text color when unset.                                                                                                                                                  |
| tileFillColor                | color string      |                | Color for the tile fill. No fill when unset.                                                                                                                                                                |
| menuSort                     | int               |                | Controls the order of the "take-" option that picks up the item, relative to other items. Items with higher priority are picked up first / appear higher up in the menu. Negative values **are** supported. |
| sound                        | [dynamic](#sound) |                | Play a sound on item drop. See [#sound](#sound) for details.                                                                                                                                                |
| icon                         | [dynamic](#icon)  |                | An icon to display next to the text overlay. See [#icon](#icon) for details.                                                                                                                                |

### `sound`

Drop sounds are configured using the `sound` display property. The value can be one of two types:
* integer e.g. `sound = 7600;` - play a sound effect directly from the game cache
  * [The wiki contains a list of known Jagex-associated "names" for sound effects](https://oldschool.runescape.wiki/w/List_of_sound_IDs).
    These effects are not "officially" named, but Jagex sometimes transmits names for them on accident during game
    updates.
* string e.g. `sound = "CustomDropSound.wav";` - play a sound effect from a user-provided sound file in `.runelite/loot-filters/sounds`.

#### custom sound files

Sound files are placed in `.runelite/loot-filters/sounds`. The plugin will create this directory for you. The folder
icon in the plugin panel will open a file browser into `.runelite/loot-filters/filters`, which you can use to quickly
navigate to other plugin directories including sounds.

**Not all formats are supported**. MP3, for example, is not supported. When in doubt, use WAV.

### `icon`

Icons are configured using the `icon` display property. The value can be any of the following "function-style"
expressions:

| name     | inputs   | example                             | description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|----------|----------|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Sprite` | int, int | `Sprite(15, 0)`<br>`Sprite(440, 2)` | Display an icon directly from the game sprite cache. Use https://abextm.github.io/cache2/#/viewer/sprite to find icons.<br><br>The 1st parameter is the "index" of the sprite as seen in the cache viewer. Most entries only have a single sprite.<br>The 2nd parameter is the "image index" within the cache entry. For example, all of the prayer icons are in a single entry at index 440. `Sprite(440, 0)` is protect from melee, `Sprite(440, 1)` is protect from missiles.<br><br>You **MUST** always provide both arguments, even if the cache entry only has one sprite. |
| `Item`   | int      | `Item(1004)`                        | Display the icon for an item using the provided item ID. Use https://chisel.weirdgloop.org/moid/ to browse items.                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `File`   | string   | `File("icon.png")`                  | Display an icon using a custom file from `.runelite/loot-filters/icons`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `CurrentItem` |  | `CurrentItem()` | Display the icon of the item itself.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |

**The plugin does not "resize" icons to fit the size of the overlay text.** At pixel sizes this low, scaling does not
look good. If there's an icon you want to use you should generally look to find one of the appropriate size in cache.
For example, sprite 0 is a 40x40px version of the Wind strike spell, which is too large, but sprite 15 is a natively
drawn 24x24px version of the same icon, which is a good size for the text overlay. This is common throughout the sprite
cache.

#### custom icons

Image files for custom icons are placed in `.runelite/loot-filters/icons`. The plugin will create this directory for
you. The folder icon in the plugin panel will open a file browser into `.runelite/loot-filters/filters`, which you can
use to quickly navigate to other plugin directories including icons.

**Not all formats are guaranteed to be supported**. PNG will definitely work.

### Color strings

Color strings can be expressed in any of the following ways:
* "#rrggbb"
* "#aarrggbb"
* "rrggbb"
* "aarrggbb"

e.g. an "orange" color would be `"ffa500"`, with or without the #. Semi-transparent orange would be `"80ffa500"`.

# Macros

Loot filters supports basic text-replacement style macros. The plugin will expand macros in the user-provided filter
before parsing it.

Macros are defined like so:

```
#define <identifier> <definition>
#define ORANGE "#ffa500"
```

or, for multi-line macros

```
#define <identifier> <line1> \
  <line2> \
  ... \
  <lineN>
```

Multi-line macros MUST end each line other than the last with a `\` character to "break" the newline.

Macros can also have parameters. For example:

```
// define
#define IMPORTANT(_name, _color) rule (name:_name) {\
  color = _color; \
  notify = true; \
}

// use
IMPORTANT("twisted bow", GREEN)
```

### Builtin macros

The scripting language includes a number of builtin macros, such as the `HIGHLIGHT` example shown above, that can be
useful for quickly scripting your own filters.

#### Basic builtins

The following static macros are defined for ease-of-use:
* RGB/CMYK primary colors (RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW, WHITE, BLACk)
* metal bar colors (e.g. BRONZE, BLURITE, IRON...) - as listed on the [wiki](https://oldschool.runescape.wiki/w/Bar#Types_of_bars)
* Default colors for each original ground items tier (GROUNDITEMS_INSANE, GROUNDITEMS_HIGH, GROUNDITEMS_MEDIUM, GROUNDITEMS_LOW)

#### Parameterized builtins

| name      | parameters     | example                              | description                                                                                           |
|-----------|----------------|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| HIGHLIGHT | name(s), color | `HIGHLIGHT("grimy*", GREEN)`         | Terminal rule, applies a simple text highlight to an item.                                            |
| HIDE      | names(s)       | `HIDE("ashes")`                      | Terminal rule, hides an item.                                                                         |
| RARE      | name(s), color | `RARE("godsword shard*", "#00ffff")` | Terminal rule, applies a text/border highlight.                                                       |
| RARE2     | name(s), color | `RARE2("goblin mail", RED)`          | Terminal rule, applies a text/border highlight w/ a semi-transparent black background and a lootbeam. |

You can see the full list of builtin macros [here](https://github.com/riktenx/loot-filters/blob/main/src/main/resources/com/lootfilters/scripts/preamble.rs2f).
