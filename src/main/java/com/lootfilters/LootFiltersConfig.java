package com.lootfilters;

import com.lootfilters.model.DespawnTimerType;
import com.lootfilters.model.DualValueDisplayType;
import com.lootfilters.model.FontMode;
import com.lootfilters.model.OverlayPriority;
import com.lootfilters.model.ValueDisplayType;
import com.lootfilters.rule.TextAccent;
import net.runelite.client.config.Alpha;
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
    @ConfigItem(keyName = "preferredDefaultFilter", hidden = true, name = "", description = "")
    default String getPreferredDefault() { return DefaultFilter.FILTERSCAPE.getName(); }
    @ConfigItem(keyName = "preferredDefaultFilter", hidden = true, name = "", description = "")
    void setPreferredDefault(String name);

    @ConfigSection(name = "Plugin panel", description = "", position = -2)
    String pluginPanel = "pluginPanel";
    String SHOW_PLUGIN_PANEL = "showPluginPanel";
    @ConfigItem(
            keyName = SHOW_PLUGIN_PANEL,
            name = "Enabled",
            description = "Show the plugin panel in the side nav. The entire plugin, including the active loot filter, will still operate if the panel is hidden.",
            section = pluginPanel
    )
    default boolean showPluginPanel() { return true; }

    @ConfigSection(
            name = "General",
            description = "Configure general options.",
            position = 0
    )
    String general = "general";
    @ConfigItem(
            keyName = "chatPrefixColor",
            name = "Chat prefix color",
            description = "Color of the chat prefix used to identify messages from this plugin.",
            section = general,
            position = -3
    )
    default Color chatPrefixColor() { return Color.decode("#00ffff"); }
    String CONFIG_KEY_FETCH_DEFAULT_FILTERS = "fetchDefaultFilters";
    @ConfigItem(
            keyName = CONFIG_KEY_FETCH_DEFAULT_FILTERS,
            name = "Fetch default filters",
            description = "Fetch the default filters and include them as options in the plugin panel.",
            section = general,
            position = -2
    )
    default boolean fetchDefaultFilters() { return true; }
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
    @ConfigItem(
            keyName = "overlayStateIndicator",
            name = "Overlay state indicator",
            description = "Show a status icon when the overlay is disabled via hotkey.",
            section = hotkey,
            position = 20
    )
    default boolean hotkeyStateIndicator() { return true; }

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
            position = 26
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

    String CONFIG_KEY_OVERLAY_PRIORITY = "overlayPriority";
    @ConfigItem(
            keyName = CONFIG_KEY_OVERLAY_PRIORITY,
            name = "Overlay priority",
            description = "Changes the draw priority of the overlay." +
            "<br>Only relative to overlays with the same order as this one.",
            section = displayOverrides,
            position = 94
    )
    default OverlayPriority overlayPriority() { return OverlayPriority.DEFAULT; }
    @ConfigItem(
            keyName = "overlayZOffset",
            name = "Overlay z-offset",
            description = "Adjusts the initial vertical offset of the text overlay.<br><br>" +
                    "This is the initial z-axis offset in 3D space. Each individual unit does not necessary correspond<br>" +
                    "to a full pixel, it will vary by camera perspective.",
            section = displayOverrides,
            position = 95
    )
    @Range(max = 32)
    default int overlayZOffset() { return 16; }
    @ConfigItem(
            keyName = "compactMode",
            name = "Compact mode",
            description = "Enable the \"compact\" overlay mode which renders icons instead of item names.<br>Certain display properties, such as the type of despawn timer, do not apply in compact mode.",
            section = displayOverrides,
            position = 100
    )
    default boolean compactMode() { return false; }
    @Range(min = 22, max = 32)
    @ConfigItem(
            keyName = "compactRenderSize",
            name = "Compact: icon height",
            description = "Icon size for compact item rendering. Specifically, height in pixels, although it will sometimes adjust it slightly to preserve aspect ratio.",
            section = displayOverrides,
            position = 101
    )
    default int compactRenderSize() { return 26; }
    @ConfigItem(
            keyName = "compactRenderRowLength",
            name = "Compact: row size",
            description = "How many items to render per row for compact items.",
            section = displayOverrides,
            position = 102
    )
    @Range(min = 1, max = 128)
    default int compactRenderRowLength() { return 4; }

    @ConfigSection(
            name = "Item lists",
            description = "Configure default lists of highlighted and hidden items. Values are case-insensitive, separated by comma. These lists are checked BEFORE the active filter.",
            position = 8
    )
    String itemLists = "itemLists";
    @ConfigItem(
            keyName = "_",
            name = "These lists take precedence over",
            description = "",
            section = itemLists,
            position = -3
    )
    void itemListsDisclaimer0();
    @ConfigItem(
            keyName = "_",
            name = "your selected loot filter.",
            description = "",
            section = itemLists,
            position = -2
    )
    void itemListsDisclaimer1();
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
    @Alpha
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
    @ConfigItem(position = 5, section = itemLists,
            keyName = "hdBackgroundColor", name = "Background", description = "")
    @Alpha default Color higlightBackgroundColor() { return null; }
    @ConfigItem(position = 6, section = itemLists,
            keyName = "hdBorderColor", name = "Border", description = "")
    @Alpha default Color highlightBorderColor() { return Color.decode("#aa00ff"); }
    @ConfigItem(position = 7, section = itemLists,
            keyName = "hdLootbeamColor", name = "Lootbeam", description = "")
    @Alpha default Color highlightLootbeamColor() { return null; }
    @ConfigItem(position = 8, section = itemLists,
            keyName = "hdMenuTextColor", name = "Menu text", description = "")
    @Alpha default Color highlightMenuTextColor() { return null; }
    @ConfigItem(position = 9, section = itemLists, keyName = "hdMenuSort", name = "Menu sort priority", description = "")
    default int highlightMenuSort() { return 0; }
    @ConfigItem(position = 10, section = itemLists,
            keyName = "hdSound", name = "Sound", description = "Can be one of two types of values:<br><br>A number: play a game sound effect by ID<br>A string: play a custom audio file from .runelite/loot-filters/sounds, not all sound formats are supported")
    default String highlightSound() { return ""; }

    @ConfigSection(
            name = "Item value rules",
            description = "These have been removed. See the readme hover below.",
            position = 99
    )
    String itemValueRules = "itemValueRules";
    @ConfigItem(
            keyName = "itemValueRulesReadme",
            name = "README (hover)",
            description = "Unlike the ground items plugin, item value rules are managed by your active loot filter.<br>Both of the default filters shipped with the plugin - FilterScape and Joe's filter - include item value tiers with thresholds similar to that of the ground items plugin.<br>You can configure both the value thresholds and display settings for these on https://filterscape.xyz/.",
            section = itemValueRules,
            position = 0
    )
    void itemValueRulesReadme();

    @ConfigSection(
            name = "Advanced",
            description = "Don't use these unless you know what you're doing.",
            position = 999
    )
    String advanced = "advanced";
    @ConfigItem(
            keyName = "showAnalyzer",
            name = "Menu: add Analyzers",
            description = "Adds an \"Analyze\" right-click menu entry for each ground item that tells you how a given" +
                    " item was evaluated against your plugin config and loot filter. Useful for debugging.",
            section = advanced,
            position = 0
    )
    default boolean showAnalyzer() { return false; }
}
