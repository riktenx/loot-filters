package com.lootfilters;

import com.lootfilters.model.DespawnTimerType;
import com.lootfilters.model.DualValueDisplayType;
import com.lootfilters.model.FontMode;
import com.lootfilters.model.ValueDisplayType;
import com.lootfilters.rule.TextAccent;
import com.lootfilters.rule.ValueTier;
import com.lootfilters.rule.ValueType;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

import java.awt.Color;

@ConfigGroup("loot-filters")
public interface LootFiltersConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "Configure general options.",
            position = 0
    )
    String general = "general";
    @ConfigItem(
            keyName = "autoToggleFilter",
            name = "Auto-toggle active filter",
            description = "Filters can be annotated with area boundaries in which they are relevant. If enabled, filters will automatically become active when the player enters their corresponding area.",
            section = general,
            position = 0
    )
    default boolean autoToggleFilters() {
        return true;
    }
    @ConfigItem(
            keyName = "showUnmatchedItems",
            name = "Show unmatched items",
            description = "Give a default text overlay to items that don't match the active filter.",
            section = general,
            position = 1
    )
    default boolean showUnmatchedItems() { return true; }
    @ConfigItem(
            keyName = "ownershipFilter",
            name = "Ownership filter",
            description = "When enabled, filters out any items you cannot pick up. This filter overrides ALL other rules/config.",
            section = general,
            position = 3
    )
    default boolean ownershipFilter() { return false; }
    @ConfigItem(
            keyName = "itemSpawnFilter",
            name = "Item spawn filter",
            description = "When enabled, filters out item spawns (world spawns, ashes from fire, etc). This filter overrides ALL other rules/config.",
            section = general,
            position = 4
    )
    default boolean itemSpawnFilter() { return false; }
    @ConfigItem(
            keyName = "valueType",
            name = "Value mode",
            description = "The type of item value to use for rules and text overlay.",
            section = general,
            position = 5
    )
    default ValueType valueType() { return ValueType.HIGHEST; }
    @ConfigItem(
            keyName = "fontMode",
            name = "Font mode",
            description = "<p>[runelite]: Respect the font type set in RuneLite -> Overlay settings -> Dynamic overlay font. Filter settings for font type will be ignored.</p><p>[plugin]: Respect the font type set by filter rules. Filter display defaults to the small font type.</p>",
            section = general,
            position = 6
    )
    default FontMode fontMode() { return FontMode.RUNELITE; }
    @ConfigItem(
            keyName = "soundVolume",
            name = "Sound volume",
            description = "Volume of sounds played by loot filter. Setting this to 0 will disable sound playback.",
            section = general,
            position = 6
    )
    @Range(max = 100)
    @Units(Units.PERCENT)
    default int soundVolume() { return 100; }

    @ConfigSection(
            name = "Hotkey",
            description = "Configure hotkey options.",
            position = 1
    )
    String hotkey = "Hotkey";
    @ConfigItem(
            keyName = "hotkey",
            name = "Hotkey",
            description = "Hotkey used by this plugin.",
            section = hotkey,
            position = 0
    )
    default Keybind hotkey() { return Keybind.ALT; }
    @ConfigItem(
            keyName = "hotkeyShowHiddenItems",
            name = "Press: Show hidden items",
            description = "Show hidden items when hotkey is pressed.",
            section = hotkey,
            position = 1
    )
    default boolean hotkeyShowHiddenItems() { return true; }
    @ConfigItem(
            keyName = "hotkeyShowClickboxes",
            name = "Press: Show hide/highlight box",
            description = "Show hide/highlight boxes when hotkey is pressed.<p>You can still toggle hide/highlight when this is disabled with left/right/middle click over an item's overlay text.",
            section = hotkey,
            position = 2
    )
    default boolean hotkeyShowClickboxes() { return true; }
    @ConfigItem(
            keyName = "hotkeyShowValues",
            name = "Press: Show item values",
            description = "Show item values when the hotkey is pressed, even if they're otherwise disabled.",
            section = hotkey,
            position = 3
    )
    default boolean hotkeyShowValues() { return false; }
    @ConfigItem(
            keyName = "hotkeyDoubleTapTogglesOverlay",
            name = "Double-tap: toggle overlay",
            description = "When enabled, double-tap the hotkey to toggle the entire ground items overlay.",
            section = hotkey,
            position = 12
    )
    default boolean hotkeyDoubleTapTogglesOverlay() { return true; }
    @ConfigItem(
            keyName = "hotkeyDoubleTapDelay",
            name = "Double-tap delay",
            description = "Period within which to register a hotkey double-tap.",
            section = hotkey,
            position = 13
    )
    @Units(Units.MILLISECONDS)
    default int hotkeyDoubleTapDelay() { return 250; }

    @ConfigSection(
            name = "Display settings",
            description = "Configure global display settings/overrides.",
            position = 2
    )
    String displayOverrides = "displayOverrides";
    @ConfigItem(
            keyName = "alwaysShowValue",
            name = "Show value",
            description = "Always show item value.",
            section = displayOverrides,
            position = 0
    )
    default boolean alwaysShowValue() { return false; }
    @ConfigItem(
            keyName = "valueDisplayType",
            name = "Value display",
            description = "Which value(s) to show.",
            section = displayOverrides,
            position = 1
    )
    default ValueDisplayType valueDisplayType() { return ValueDisplayType.HIGHEST; }
    @ConfigItem(
            keyName = "dualValueDisplayType",
            name = "Dual-value display",
            description = "How to compose the display of values when display type is set to either 'highest' or 'both'.",
            section = displayOverrides,
            position = 2
    )
    default DualValueDisplayType dualValueDisplay() { return DualValueDisplayType.COMPACT; }
    @ConfigItem(
            keyName = "alwaysShowDespawn",
            name = "Show despawn",
            description = "Always show item despawn timers.",
            section = displayOverrides,
            position = 11
    )
    default boolean alwaysShowDespawn() { return false; }
    @ConfigItem(
            keyName = "despawnTimerType",
            name = "Despawn type",
            description = "Type of despawn timer to render.",
            section = displayOverrides,
            position = 12
    )
    default DespawnTimerType despawnTimerType() { return DespawnTimerType.TICKS; }
    @ConfigItem(
            keyName = "despawnThreshold",
            name = "Despawn threshold",
            description = "Number of remaining ticks until despawn at which to show the despawn timer (0 to always show).",
            section = displayOverrides,
            position = 13
    )
    @Units(Units.TICKS)
    default int despawnThreshold() { return 0; }
    @ConfigItem(
            keyName = "textAccent",
            name = "Text accent",
            description = "Text accent type.",
            section = displayOverrides,
            position = 23
    )
    default TextAccent textAccent() { return TextAccent.USE_FILTER; }
    @ConfigItem(
            keyName = "highlightTiles",
            name = "Highlight tiles",
            description = "Always highlight tiles, regardless of filter config.",
            section = displayOverrides,
            position = 24
    )
    default boolean highlightTiles() { return false; }
    @ConfigItem(
            keyName = "collapseEntries",
            name = "Menu: collapse entries",
            description = "Collapse menu entries for multiples of unstacked items.",
            section = displayOverrides,
            position = 89
    )
    default boolean collapseEntries() { return true; }
    @ConfigItem(
            keyName = "deprioritizeHidden",
            name = "Menu: deprioritize hidden items",
            description = "Deprioritize menu entries for hidden items.",
            section = displayOverrides,
            position = 90
    )
    default boolean deprioritizeHidden() { return false; }
    @ConfigItem(
            keyName = "recolorHidden",
            name = "Menu: recolor hidden items",
            description = "Recolor menu entries for hidden items.",
            section = displayOverrides,
            position = 91
    )
    default boolean recolorHidden() { return false; }
    @ConfigItem(
            keyName = "hiddenColor",
            name = "Hidden color",
            description = "Color for hidden items in text overlay and menu entries.",
            section = displayOverrides,
            position = 92
    )
    default Color hiddenColor() { return Color.GRAY; }

    @ConfigSection(
            name = "Item lists",
            description = "Configure default lists of highlighted and hidden items. Values are case-insensitive, separated by comma. These lists are checked BEFORE the active filter.",
            position = 8
    )
    String itemLists = "itemLists";
    @ConfigItem(
            keyName = "highlightedItems",
            name = "Highlighted items",
            description = "Configure a list of items to highlight.",
            section = itemLists,
            position = 0
    )
    default String highlightedItems() { return ""; }
    @ConfigItem(
            keyName = "hiddenItems",
            name = "Hidden items",
            description = "Configure a list of items to hide.",
            section = itemLists,
            position = 1
    )
    default String hiddenItems() { return ""; }
    @ConfigItem(keyName = "highlightedItems", name = "", description = "")
    void setHighlightedItems(String key);
    @ConfigItem(keyName = "hiddenItems", name = "", description = "")
    void setHiddenItems(String key);

    @ConfigItem(
            keyName = "highlightColor",
            name = "Highlight color",
            description = "Configures the color for highlighted items.",
            section = itemLists,
            position = 2
    )
    default Color highlightColor() { return Color.decode("#aa00ff"); }
    @ConfigItem(
            keyName = "highlightLootbeam",
            name = "Highlight lootbeam",
            description = "Configures whether highlighted items show a lootbeam.",
            section = itemLists,
            position = 3
    )
    default boolean highlightLootbeam() { return false; }
    @ConfigItem(
            keyName = "highlightNotify",
            name = "Highlight notification",
            description = "Configures whether highlighted items fire a system notification.",
            section = itemLists,
            position = 4
    )
    default boolean highlightNotify() { return false; }

    @ConfigSection(
            name = "Item value rules",
            description = "Configure default rules for showing based on item value. These rules are checked AFTER both the active filter and the global highlight/hide lists.",
            position = 9
    )
    String itemValueRules = "itemValueRules";
    @ConfigItem(
            keyName = "lootbeamTier",
            name = "Lootbeam tier",
            description = "Minimum tier at which to show a lootbeam.",
            section = itemValueRules,
            position = 0
    )
    default ValueTier lootbeamTier() { return ValueTier.HIGH; }
    @ConfigItem(
            keyName = "notifyTier",
            name = "Notification tier",
            description = "Minimum tier at which to fire a system notification.",
            section = itemValueRules,
            position = 0
    )
    default ValueTier notifyTier() { return ValueTier.HIGH; }
    @ConfigItem(
            keyName = "enableInsaneItemValueTier",
            name = "Insane tier",
            description = "Enable INSANE item value tier.",
            section = itemValueRules,
            position = 11
    )
    default boolean enableInsaneItemValueTier() { return true; }
    @ConfigItem(
            keyName = "insaneValue",
            name = "Insane value",
            description = "Configures the value for INSANE tier.",
            section = itemValueRules,
            position = 12
    )
    default int insaneValue() { return 10_000_000; }
    @ConfigItem(
            keyName = "insaneValueColor",
            name = "Insane color",
            description = "Configures the color for INSANE item values.",
            section = itemValueRules,
            position = 13
    )
    default Color insaneValueColor() { return Color.decode("#ff66b2"); }
    @ConfigItem(
            keyName = "enableHighItemValueTier",
            name = "High tier",
            description = "Enable high item value tier.",
            section = itemValueRules,
            position = 14
    )
    default boolean enableHighItemValueTier() { return true; }
    @ConfigItem(
            keyName = "highValue",
            name = "High value",
            description = "Configures the value for high tier.",
            section = itemValueRules,
            position = 15
    )
    default int highValue() { return 1_000_000; }
    @ConfigItem(
            keyName = "highValueColor",
            name = "High color",
            description = "Configures the color for high item values.",
            section = itemValueRules,
            position = 16
    )
    default Color highValueColor() { return Color.decode("#ff9600"); }
    @ConfigItem(
            keyName = "enableMediumItemValueTier",
            name = "Medium tier",
            description = "Enable medium item value tier.",
            section = itemValueRules,
            position = 17
    )
    default boolean enableMediumItemValueTier() { return true; }
    @ConfigItem(
            keyName = "mediumValue",
            name = "Medium value",
            description = "Configures the value for medium tier.",
            section = itemValueRules,
            position = 18
    )
    default int mediumValue() { return 100_000; }
    @ConfigItem(
            keyName = "mediumValueColor",
            name = "Medium color",
            description = "Configures the color for medium item values.",
            section = itemValueRules,
            position = 19
    )
    default Color mediumValueColor() { return Color.decode("#99ff99"); }
    @ConfigItem(
            keyName = "enableLowItemValueTier",
            name = "Low tier",
            description = "Enable low item value tier.",
            section = itemValueRules,
            position = 20
    )
    default boolean enableLowItemValueTier() { return true; }
    @ConfigItem(
            keyName = "lowValue",
            name = "Low value",
            description = "Configures the value for low tier.",
            section = itemValueRules,
            position = 21
    )
    default int lowValue() { return 10_000; }
    @ConfigItem(
            keyName = "lowValueColor",
            name = "Low color",
            description = "Configures the color for low item values.",
            section = itemValueRules,
            position = 22
    )
    default Color lowValueColor() { return Color.decode("#66b2ff"); }
    @ConfigItem(
            keyName = "hiddenTierEnabled",
            name = "Hide below value tier",
            description = "Hide items below a certain value.",
            section = itemValueRules,
            position = 23
    )
    default boolean hideTierEnabled() { return false; }
    @ConfigItem(
            keyName = "hiddenValue",
            name = "Hide below value",
            description = "Hide items below this value, if enabled.",
            section = itemValueRules,
            position = 24
    )
    default int hideTierValue() { return 0; }
    @ConfigItem(
            keyName = "hiddenNoHideUntradeables",
            name = "Don't hide untradeables",
            description = "Ignore untradeables for the hide-below-value tier.",
            section = itemValueRules,
            position = 25
    )
    default boolean hideTierShowUntradeable() { return true; }
}
