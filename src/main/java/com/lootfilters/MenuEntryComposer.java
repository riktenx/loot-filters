package com.lootfilters;

import lombok.AllArgsConstructor;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lootfilters.util.TextUtil.abbreviate;
import static net.runelite.client.util.ColorUtil.colorTag;

@AllArgsConstructor
public class MenuEntryComposer {
    private final LootFiltersPlugin plugin;

    public void onMenuEntryAdded(MenuEntry entry) { // recolor/add quantity
        if (!isGroundItem(entry)) {
            return;
        }

        var item = getItemForEntry(entry);
        var match = plugin.getActiveFilter().findMatch(plugin, item);
        if (match == null) {
            entry.setTarget(buildTargetText(item, DisplayConfig.DEFAULT_MENU_TEXT_COLOR));
            return;
        }

        entry.setDeprioritized(plugin.getConfig().deprioritizeHidden() && match.isHidden());
        var color = match.isHidden() && plugin.getConfig().recolorHidden()
                ? plugin.getConfig().hiddenColor()
                : match.getMenuTextColor();
        entry.setTarget(buildTargetText(item, color));
    }

    public void onMenuOpened() { // collapse
        var menu = plugin.getClient().getMenu();
        var entries = menu.getMenuEntries();

        var itemCounts = Stream.of(entries)
                .filter(MenuEntryComposer::isGroundItem)
                .collect(Collectors.groupingBy(MenuEntryComposer::entrySlug, Collectors.counting()));

        var newEntries = Arrays.stream(entries)
                .map(it -> isGroundItem(it)
                        ? withCount(it, itemCounts.getOrDefault(entrySlug(it), 1L))
                        : it)
                .distinct()
                .toArray(MenuEntry[]::new);
        menu.setMenuEntries(newEntries);
    }

    private MenuEntry withCount(MenuEntry entry, long count) {
        return count > 1
                ? entry.setTarget(entry.getTarget() + " x" + count)
                : entry;
    }

    private TileItem getItemForEntry(MenuEntry entry) {
        var wv = plugin.getClient().getTopLevelWorldView();
        var point = WorldPoint.fromScene(wv, entry.getParam0(), entry.getParam1(), wv.getPlane());
        return plugin.getTileItemIndex().findItem(point, entry.getIdentifier());
    }

    private String buildTargetText(TileItem item, Color color) {
        var text = plugin.getItemName(item.getId());
        if (item.getQuantity() > 1) {
            text += " (" + abbreviate(item.getQuantity()) + ")";
        }
        return colorTag(color) + text;
    }

    private static boolean isGroundItem(MenuEntry entry) {
        var type = entry.getType();
        return type == MenuAction.GROUND_ITEM_FIRST_OPTION
                || type == MenuAction.GROUND_ITEM_SECOND_OPTION
                || type == MenuAction.GROUND_ITEM_THIRD_OPTION
                || type == MenuAction.GROUND_ITEM_FOURTH_OPTION
                || type == MenuAction.GROUND_ITEM_FIFTH_OPTION
                || type == MenuAction.EXAMINE_ITEM_GROUND;
    }

    private static String entrySlug(MenuEntry entry) {
        return entry.getType().toString() + entry.getIdentifier();
    }
}
