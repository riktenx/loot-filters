# Loot Filters

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

// we really care about anglerfish
if (name:"anglerfish") {
  color = MAGENTA;
}

if (name:"coins") {
  color = YELLOW; // and also, coins
  showLootbeam = true;
}
```

Scriptable filters allow us to exercise both a deep and far-reaching level of control over how to display ground items.

## Whitespace and comments

Single-line comments are supported, delimited by `//`, and can appear anywhere on a line, as demonstrated above. The
parser will ignore all text from the comment marker until the end of the line.

Block-style comments, e.g. `/* block */` are not supported at this time.

The script parser will ignore whitespace in the way you'd expect it to for any other language. For example, these two
matcher expressions are semantically equivalent:

```
// relaxed
if (value:>100) {
  color = BLUE;
}

if (value:>100) {color = BLUE;} // compact
```

## The `meta` block

Metadata for a filter is given in a special metadata block. This should generally be placed at the top of the script
text, although the parser doesn't know the difference.

```
meta {
    name = "riktenx/demo";
    description = "demo filter";
    area = [1,2,3,4,5,6];
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

#### `area`

The in-game area, expressed in coordinates, in which the plugin should automatically load this filter. This mainly
allows you to write bespoke filters for specific bosses.

Area is expressed as a list of _exactly_ six (6) integers that represent the boundary coordinates of the desired
activation area:

```
[x0, y0, z0, x1, y1, z1]
```

There are several excellent web-based tools for finding map coordinates, such as https://mejrs.github.io/osrs.

For example, the coordinate pair `[2240, 4032, 0, 2303, 4095, 0]` describes the area for Vorkath.

## Rules

A loot filter is written as a list of rules, like so:

```
(if|apply) (<conditions...>) {
    <display_property1> = <value>;
    <display_property2> = <value>;
    ...
}
... more rules
```

There are **two (2)** top-level types of rules:
* TERMINAL rules, expressed via the `if` keyword. The filter stops evaluating for an item when it matches a non-terminal
  rule.
* NON-TERMINAL rules, expressed via the `apply` keyword. Applies any display settings to the item when it matches, but
  keeps evaluating the script.

Rules evaluate top-down for a given ground item. The plugin terminates evaluation when a non-terminal rule with matching
conditions is encountered.

### Example: terminal vs non-terminal

Consider the following filter:

```
apply (value:>100_000) {
  borderColor = BLUE;
  showLootbeam = true;
}

if (name:"*godsword") {
  textColor = "#00ffff";
}

if (name:"bandos*") {
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

Rules support the following conditions:

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

Match based on an item value. Supports the same comparison operators used in `quantity`.

The value used for comparison is determined by plugin settings (GE, HA, or highest).

#### tradeable `tradeable:true` or `tradeable:false`

Match based on whether an item is tradeable.

#### stackable `stackable:true` or `stackable:false`

Match based on whether an item is stackable (noted, coins, fishbait etc.).

#### noted `noted:true` or `noted:false`

Match based on whether an item is a note.

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
if (name:"coins" && quantity:>100_000) {
  textColor = YELLOW;
  borderColor = YELLOW;
  showLootbeam = true;
  lootbeamColor = "#ffffff";
}
```

The following table lists supported display settings:

| name                         | value type   | ordinal macros | description                                                                                                                         |
|------------------------------|--------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------|
| hidden                       | boolean      |                | Whether this item is comprehensively hidden. Setting hidden to true disables EVERYTHING else - no lootbeam, no tile highlight, etc. |
| hideOverlay                  | boolean      |                | Whether this item is hidden in the overlay. Disables the text overlay but allows other features to work.                            |
| color, textColor             | color string |                | Color for the display text of the item.                                                                                             |
| backgroundColor              | color string |                | Background color behind the display text.                                                                                           |
| borderColor                  | color string |                | Border color around the display text.                                                                                               |
| showLootbeam, showLootBeam   | boolean      |                | Show an in-world lootbeam on the item's tile.                                                                                       |
| showValue                    | boolean      |                | Include an item's value in the text overlay. The highest value between GE and HA price is chosen.                                   |
| showDespawn                  | boolean      |                | Show a despawn timer next to the text overlay. The type of despawn timer is controlled by config.                                   |
| notify                       | boolean      |                | Fire a system notification when the matched item drops.                                                                             |
| textAccent                   | enum         | `TEXTACCENT_*` | Text accent to use:<li>1 = text shadow (default)</li><li>2 = outline</li><li>3 = none</li>                                          |
| textAccentColor              | color string |                | Color for the text accent. Defaults to solid black.                                                                                 |
| lootbeamColor, lootBeamColor | color string |                | Color for the lootbeam. Defaults to the text color when unset.                                                                      |
| fontType                     | enum         | `FONTTYPE_*`   | Font used for the display:<li>1 = normal overlay text size (default)</li><li>2 = larger                                             |
| menuTextColor                | color string |                | Color for the menu entry text. Defaults to the text color when unset.                                                               | 
| highlightTile                | boolean      |                | Whether to highlight the tile the item is on.                                                                                       | 
| tileStrokeColor              | color string |                | Color for tile outline. Defaults to text color when unset.                                                                          |
| tileFillColor                | color string |                | Color for the tile fill. No fill when unset.                                                                                        |

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
#define IMPORTANT(_name, _color) if (name:_name) {\
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
| RARE2     | name(s), color | `RARE2("goblin mail", RED")`         | Terminal rule, applies a text/border highlight w/ a semi-transparent black background and a lootbeam. |

You can see the full list of builtin macros [here](https://github.com/riktenx/loot-filters/blob/main/src/main/resources/com/lootfilters/scripts/preamble.rs2f).
