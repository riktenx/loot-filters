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

@ConfigGroup(LootFiltersConfig.CONFIG_GROUP)
public interface LootFiltersConfig extends Config {
    String CONFIG_GROUP = "loot-filters";

    // Sections

    @ConfigSection(
            name = "General",
            description = "Configure general settings.",
            position = 0
    )
    String SECTION_GENERAL = "general";

    @ConfigSection(
            name = "Hotkey",
            description = "Configure hotkey settings.",
            position = 1
    )
    String SECTION_HOTKEY = "Hotkey";

    @ConfigSection(
            name = "Global display settings",
            description = "Configure global display settings.",
            position = 2
    )
    String SECTION_DISPLAY_SETTINGS = "displayOverrides";

    @ConfigSection(
            name = "Item lists",
            description = "Configure highlighted and hidden item list settings.",
            position = 3
    )
    String SECTION_ITEM_LISTS = "itemLists";

    @ConfigSection(
            name = "Advanced highlight display",
            description = "Configure highlighted item display settings.",
            position = 4
    )
    String SECTION_HIGHLIGHT_DISPLAY = "Highlight display";

    @ConfigSection(
            name = "Item value rules",
            description = "These have been removed. See the readme hover below.",
            position = 99
    )
    String SECTION_ITEM_VALUE_RULES = "itemValueRules";

    // General Section

    String SHOW_PLUGIN_PANEL = "showPluginPanel";

    @ConfigItem(
            keyName = SHOW_PLUGIN_PANEL,
            name = "Plugin panel icon",
            description = "Toggle the plugin panel icon in the side navigation menu." +
                    "<br>The selected filter is still active when the icon is hidden.",
            section = SECTION_GENERAL,
            position = 0
    )
    default boolean showPluginPanel() {
        return true;
    }

    String CONFIG_KEY_FETCH_DEFAULT_FILTERS = "fetchDefaultFilters";

    @ConfigItem(
            keyName = CONFIG_KEY_FETCH_DEFAULT_FILTERS,
            name = "Default filters",
            description = "Fetch the default filters and include them as options in the plugin panel.",
            section = SECTION_GENERAL,
            position = 1
    )
    default boolean fetchDefaultFilters() {
        return true;
    }

    @ConfigItem(
            keyName = "autoToggleFilter",
            name = "Auto-switch area filters",
            description = "Area-based filters will activate/deactivate when the player enters or leaves a zone.",
            section = SECTION_GENERAL,
            position = 2
    )
    default boolean autoToggleFilters() {
        return true;
    }

    @ConfigItem(
            keyName = "ownershipFilter",
            name = "Ownership filter",
            description = "Filter out items you cannot pick up."
                    + "<br>This global setting takes precedence over the active filter.",
            section = SECTION_GENERAL,
            position = 3
    )
    default boolean ownershipFilter() {
        return false;
    }

    @ConfigItem(
            keyName = "itemSpawnFilter",
            name = "Item spawn filter",
            description = "Filter out item spawns (world spawns, ashes from fire, etc)."
                    + "<br>This global setting takes precedence over the active filter.",
            section = SECTION_GENERAL,
            position = 4
    )
    default boolean itemSpawnFilter() {
        return false;
    }

    @ConfigItem(
            keyName = "showUnmatchedItems",
            name = "Default item text",
            description = "Display a default text overlay for items not handled by the active filter.",
            section = SECTION_GENERAL,
            position = 5
    )
    default boolean showUnmatchedItems() {
        return true;
    }

    @ConfigItem(
            keyName = "fontMode",
            name = "Font mode",
            description = "Runelite: Use the 'Dynamic overlay font' config setting in the RuneLite plugin."
                    + "<br>Plugin: Use the font type set by the active filter. Defaults to Small.",
            section = SECTION_GENERAL,
            position = 6
    )
    default FontMode fontMode() {
        return FontMode.RUNELITE;
    }

    @ConfigItem(
            keyName = "soundVolume",
            name = "Sound volume",
            description = "Volume of any sounds played by the active filter." +
                    "<br>Setting this to 0 will disable sound playback.",
            section = SECTION_GENERAL,
            position = 7
    )
    @Range(max = 100)
    @Units(Units.PERCENT)
    default int soundVolume() {
        return 100;
    }

    // Hotkey Section

    @ConfigItem(
            keyName = "hotkey",
            name = "Hotkey",
            description = "Hotkey used by this plugin.",
            section = SECTION_HOTKEY,
            position = 0
    )
    default Keybind hotkey() {
        return Keybind.ALT;
    }

    @ConfigItem(
            keyName = "hotkeyShowHiddenItems",
            name = "Press: Show hidden items",
            description = "Show hidden items when hotkey is pressed.",
            section = SECTION_HOTKEY,
            position = 1
    )
    default boolean hotkeyShowHiddenItems() {
        return true;
    }

    @ConfigItem(
            keyName = "hotkeyShowClickboxes",
            name = "Press: Show hide/highlight box",
            description = "Show hide/highlight boxes when hotkey is pressed."
                    + "<br>Alternatively, left/right/middle mouse click an item's overlay text to toggle hiding/highlighting.",
            section = SECTION_HOTKEY,
            position = 2
    )
    default boolean hotkeyShowClickboxes() {
        return true;
    }

    @ConfigItem(
            keyName = "hotkeyShowValues",
            name = "Press: Show item values",
            description = "Show item values when the hotkey is pressed.",
            section = SECTION_HOTKEY,
            position = 3
    )
    default boolean hotkeyShowValues() {
        return false;
    }

    @ConfigItem(
            keyName = "hotkeyDoubleTapTogglesOverlay",
            name = "Double-tap: Toggle overlay",
            description = "Double-tap the hotkey to toggle the entire ground items overlay.",
            section = SECTION_HOTKEY,
            position = 4
    )
    default boolean hotkeyDoubleTapTogglesOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "hotkeyDoubleTapDelay",
            name = "Double-tap delay",
            description = "Input timeout to register a hotkey double-tap.",
            section = SECTION_HOTKEY,
            position = 5
    )
    @Units(Units.MILLISECONDS)
    default int hotkeyDoubleTapDelay() {
        return 250;
    }

    // Display Settings Section

    @ConfigItem(
            keyName = "alwaysShowValue",
            name = "Item values",
            description = "Show item values in text overlays." +
                    "<br>This global setting takes precedence over the active filter.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 0
    )
    default boolean alwaysShowValue() {
        return false;
    }

    @ConfigItem(
            keyName = "valueDisplayType",
            name = "Item value type",
            description = "The type of item value to show in text overlays.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 1
    )
    default ValueDisplayType valueDisplayType() {
        return ValueDisplayType.HIGHEST;
    }

    @ConfigItem(
            keyName = "dualValueDisplayType",
            name = "Item value mode",
            description = "Applies when item value type is set to 'highest' or 'both'.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 2
    )
    default DualValueDisplayType dualValueDisplay() {
        return DualValueDisplayType.COMPACT;
    }

    @ConfigItem(
            keyName = "alwaysShowDespawn",
            name = "Item despawn timers",
            description = "Show item despawn timers." +
                    "<br>This global setting takes precedence over the active filter.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 3
    )
    default boolean alwaysShowDespawn() {
        return false;
    }

    @ConfigItem(
            keyName = "despawnTimerType",
            name = "Despawn timer type",
            description = "Type of despawn timer to show.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 4
    )
    default DespawnTimerType despawnTimerType() {
        return DespawnTimerType.TICKS;
    }

    @ConfigItem(
            keyName = "despawnThreshold",
            name = "Despawn threshold",
            description = "Number of remaining ticks until despawn at which to show the despawn timer."
                    + "<br>Setting this to 0 will always show the despawn timer, when enabled.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 5
    )
    @Units(Units.TICKS)
    default int despawnThreshold() {
        return 0;
    }

    @ConfigItem(
            keyName = "textAccent",
            name = "Text accent",
            description = "Text accent type for item overlays." +
                    "<br>This global setting takes precedence over/defers to the active filter.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 6
    )
    default TextAccent textAccent() {
        return TextAccent.USE_FILTER;
    }

    @ConfigItem(
            keyName = "highlightTiles",
            name = "Highlight item tiles",
            description = "Outline the tile of an item using the same color as its text overlay." +
                    "<br>This global setting takes precedence over the active filter.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 7
    )
    default boolean highlightTiles() {
        return false;
    }

    @ConfigItem(
            keyName = "collapseEntries",
            name = "Menu: collapse entries",
            description = "Collapse menu entries for multiples of unstacked items.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 8
    )
    default boolean collapseEntries() {
        return true;
    }

    @ConfigItem(
            keyName = "deprioritizeHidden",
            name = "Menu: deprioritize hidden items",
            description = "Deprioritize menu entries for hidden items.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 9
    )
    default boolean deprioritizeHidden() {
        return false;
    }

    @ConfigItem(
            keyName = "recolorHidden",
            name = "Menu: recolor hidden items",
            description = "Recolor menu entries for hidden items.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 10
    )
    default boolean recolorHidden() {
        return false;
    }

    @ConfigItem(
            keyName = "hiddenColor",
            name = "Hidden item color",
            description = "Color for hidden item text overlays and menu entries.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 11
    )
    default Color hiddenColor() {
        return Color.GRAY;
    }

    String CONFIG_KEY_OVERLAY_PRIORITY = "overlayPriority";

    @ConfigItem(
            keyName = CONFIG_KEY_OVERLAY_PRIORITY,
            name = "Overlay priority",
            description = "Change the draw priority of item overlays." +
                    "<br>Allows this plugin's overlays to be drawn above/below other plugins.",
            section = SECTION_DISPLAY_SETTINGS,
            position = 12
    )
    default OverlayPriority overlayPriority() {
        return OverlayPriority.DEFAULT;
    }

    // Item Lists Section

    @ConfigItem(keyName = "highlightedItems", name = "", description = "")
    void setHighlightedItems(String key);

    @ConfigItem(
            keyName = "highlightedItems",
            name = "Highlighted items",
            description = "A list of item names to highlight." +
                    "<br>Names are case-insensitive and must be comma-separated." +
                    "<br>Supports less/greater than operators, e.g. coins>1000" +
                    "<br>This global setting takes precedence over the active filter.",
            section = SECTION_ITEM_LISTS,
            position = 0
    )
    default String highlightedItems() {
        return "";
    }

    @ConfigItem(keyName = "hiddenItems", name = "", description = "")
    void setHiddenItems(String key);

    @ConfigItem(
            keyName = "hiddenItems",
            name = "Hidden items",
            description = "A list of item names to hide." +
                    "<br>Names are case-insensitive and must be comma-separated." +
                    "<br>This global setting takes precedence over the active filter.",
            section = SECTION_ITEM_LISTS,
            position = 1
    )
    default String hiddenItems() {
        return "";
    }

    @ConfigItem(
            keyName = "highlightColor",
            name = "Highlight color",
            description = "Text overlay color for highlighted items.",
            section = SECTION_ITEM_LISTS,
            position = 2
    )
    default Color highlightColor() {
        return Color.decode("#aa00ff");
    }

    @ConfigItem(
            keyName = "highlightLootbeam",
            name = "Highlight lootbeam",
            description = "Show a lootbeam for highlighted items." +
                    "<br>The lootbeam color will match the text overlay color.",
            section = SECTION_ITEM_LISTS,
            position = 3
    )
    default boolean highlightLootbeam() {
        return false;
    }

    @ConfigItem(
            keyName = "highlightNotify",
            name = "Highlight notification",
            description = "Fire a system notification for highlighted items.",
            section = SECTION_ITEM_LISTS,
            position = 4
    )
    default boolean highlightNotify() {
        return false;
    }

    // Highlight Display Section

    @ConfigItem(position = 0, section = SECTION_HIGHLIGHT_DISPLAY,
            keyName = "hdBackgroundColor", name = "Background", description = "")
    @Alpha
    default Color higlightBackgroundColor() {
        return null;
    }

    @ConfigItem(position = 1, section = SECTION_HIGHLIGHT_DISPLAY,
            keyName = "hdBorderColor", name = "Border", description = "")
    @Alpha
    default Color highlightBorderColor() {
        return null;
    }

    @ConfigItem(position = 2, section = SECTION_HIGHLIGHT_DISPLAY,
            keyName = "hdLootbeamColor", name = "Lootbeam", description = "")
    @Alpha
    default Color highlightLootbeamColor() {
        return null;
    }

    @ConfigItem(position = 3, section = SECTION_HIGHLIGHT_DISPLAY,
            keyName = "hdMenuTextColor", name = "Menu text", description = "")
    @Alpha
    default Color highlightMenuTextColor() {
        return null;
    }

    @ConfigItem(position = 4, section = SECTION_HIGHLIGHT_DISPLAY, keyName = "hdMenuSort", name = "Menu sort priority", description = "")
    default int highlightMenuSort() {
        return 0;
    }

    @ConfigItem(position = 9, section = SECTION_HIGHLIGHT_DISPLAY,
            keyName = "hdSound", name = "Sound", description = "Can be one of two types of values:<br><br>A number: play a game sound effect by ID<br>A string: play a custom audio file from .runelite/loot-filters/sounds, not all sound formats are supported")
    default String highlightSound() {
        return "";
    }

    // Item Value Rules Section

    @ConfigItem(
            keyName = "itemValueRulesReadme",
            name = "Readme (hover over this)",
            description = "Unlike the ground items plugin, item value rules are managed by your active loot filter." +
                    "<br>Both of the default filters shipped with the plugin - FilterScape and Joe's filter - include item value tiers with thresholds similar to that of the ground items plugin." +
                    "<br>You can configure both the value thresholds and display settings for these on https://filterscape.xyz" +
                    "<br><br>This toggle has no effect.",
            section = SECTION_ITEM_VALUE_RULES,
            position = 0
    )
    default boolean itemValueRulesReadme() {
        return false;
    }
}
