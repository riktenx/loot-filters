# Loot Filters

## Overview

A loot filter is a basic script that contains the following
* Self-identifying metadata (filter name, description, etc.)
* Any number of **matchers** - each being a tuple of condition and display config - that determines how to display items
  on the ground.

For example, this is a simple loot filter with two matchers: one that highlights anglerfish, and another that highlights
coins with a lootbeam:

```
meta {
  name = "riktenx/test";
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

There are several excellent web-based tools for finding map coordinates:
* https://explv.github.io/
* https://mejrs.github.io/

For example, the coordinate pair `[2240, 4032, 0, 2303, 4095, 0]`
[describes the area for Vorkath](https://explv.github.io/?centreX=2284&centreY=4084&centreZ=0&zoom=8).

## Matchers

A loot filter is written as a list of matchers. A matcher of a combination of conditions and display settings that
controls when and how the text overlay for an item is displayed.

Matchers are expressed as

```
if (<conditions...>) {
    <display_property1> = <value>;
    <display_property2> = <value>;
    ...
}
```

Matchers evaluate top-down for a given ground item. The plugin selects display settings for the first matcher with rules
that match an item being tested.

### Conditions

Conditions are expressed in the form

```
<condition type>:<arguments>
```

You can use logical operators to express compound conditions, such as

```
name:"blue dragonhide" && quantity:>1
```

Matchers support the following conditions:

#### name `name:"..."`

Match based on an item's name, case-insensitive. You can also match with a wildcard `*` on either side of the name (
e.g. `"* dragonhide"` matches all dragonhide colors).

#### id `id:995`

Match based on an exact item ID.

#### quantity `quantity:>500`

Match based on an item's quantity.

#### value `value:>500`

Match based on an item value. The value used for comparison is determined by plugin settings (GE, HA, or highest).

### Display settings

The following table lists the supported display settings for matchers:

| name             | value type              | ordinal macros | description                                                                                          |
|------------------|-------------------------|----------------|------------------------------------------------------------------------------------------------------|
| hidden           | boolean                 |                | Whether this item is hidden in the overlay. When set to true, other display settings have no effect. |
| color, textColor | string (ARGB color hex) |                | Color for the display text of the item.                                                              |
| backgroundColor  | string (ARGB color hex) |                | Background color behind the display text.                                                            |
| borderColor      | string (ARGB color hex) |                | Border color around the display text.                                                                |
| showLootbeam     | boolean                 |                | Show an in-world lootbeam on the item's tile. The lootbeam color matches the configured text color.  |
| showValue        | boolean                 |                | Include an item's value in the text overlay. The highest value between GE and HA price is chosen.    |
| showDespawn      | boolean                 |                | Show a despawn timer, in game ticks, next to the text overlay.                                       |
| notify           | boolean                 |                | Fire a system notification when the matched item drops.                                              |
| textAccent       | enum                    | `TEXTACCENT_*` | Text accent to use:<li>1 = text shadow (default)</li><li>2 = outline</li>                            |

## Text macros

Loot filters supports basic text-replacement style macros. The plugin will expand macros in the user-provided filter
before parsing it for matchers.

Macros are defined like so:

```
#define ORANGE "ffFFA500"
if (value:>1000) {
    color = ORANGE;
}
```

Macros can also have parameters. For example:

```
#define HIGHLIGHT(_name, _color) if (name:_name) { color = _color; }
HIGHLIGHT("dragon arrowtips", "ffff0000")
```

Expands to

```
if (name:"dragon arrowtips") { color = "ffff0000"; }
```

Note that you can override a previous definition of a macro by re-defining it on a subsequent line.

### Multi-line macros

You can use a backslash at the end of a line, any number of times, to "continue" a macro definition:

```
#define MATCH_VALUE_MULTILINE(_name, _vexpr, _color) if (name:_name && value:_vexpr) { \
  color = _color; \
  borderColor = _color; \
}
```

A comment **CANNOT** appear after an EOL backslash.

### Builtin macros

The scripting language includes a number of builtin macros, such as the `HIGHLIGHT` example shown above, that can be
useful for quickly scripting your own filters. This is accomplished by pre-pending a "preamble" script to the
user-provided filter. You can see the full list of macros defined in the preamble
[here](https://github.com/riktenx/loot-filters/blob/main/src/main/resources/com/lootfilters/scripts/preamble.rs2f).