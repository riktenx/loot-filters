# `riktenx/loot-filters`

![](https://github.com/riktenx/loot-filters/blob/main/readme-images/header_new.png)

Loot Filters (AKA "Improved Ground Items") is an extended, highly-customizable replacement for Runelite's built-in
ground items plugin.

Join the Discord! https://discord.gg/ESbA28wPnt

## Overview

This plugin replicates the majority of the experience offered by the base version:
* ALT + mouse buttons toggle item hide/highlight (left click to highlight, right click to hide, middle click to reset)
* Text overlay for ground items
* Configure display colors based on item value
* Collapse duplicate right-click menu entries
* De-prioritize menu entries for hidden items
* Lootbeams

## Additions

The plugin provides the following extensions over the base Ground Items experience:
* Streamlined text overlay
* Streamlined configuration
* **Scriptable rules language, A.K.A. "Loot Filters" for fine-grained control over display and color settings for items
  on the ground.**
  * Save/load multiple filters
  * Import current config (highlight, hide, and value rules) into a new filter
  * Automatically toggle active filter based on your position in the game world (e.g. for bossing)

## Loot Filters

The primary addition in this plugin is the introduction of a simple but powerful scripting language to control when and
how items on the ground are displayed. An individual script is known as a **loot filter**.

Loot filters are essentially a list of **matchers**: pairs of **item rules** and **display settings**, which controls the
text overlay for items on the ground.

### Matchers

Filters can match on various characteristics of an item:
* name / id
* value
* quantity of stacked items
* whether an item is tradeable
* whether an item is stackable
* whether an item is noted

Items that match a particular set of conditions can be configured to display in various ways:
* show/hide
* display colors - text, background, border, and more

* show a lootbeam
* notifications

The teaser image at the top of this README provides an example of what is possible with loot filters.

The scripting language resembles something like C, but you shouldn't need any experience with computer programming to
design your own loot filters.

Loot filters work together with the basic config options to decide how to render text overlays for items on the ground.
Additionally, you can capture the current state of your config - your highlighted/hidden items, and your item value
rules - and save them to a new filter which you can then re-load at a later date. This allows you to configure different
filters for different in-game activities.

For a comprehensive reference for writing your own loot filters, see [this guide](https://github.com/riktenx/loot-filters/blob/main/guides/loot-filters.md).
