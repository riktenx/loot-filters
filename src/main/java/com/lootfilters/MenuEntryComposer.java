package com.lootfilters;

import lombok.RequiredArgsConstructor;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lootfilters.util.CollectionUtil.findBounds;
import static net.runelite.client.util.ColorUtil.colorTag;

@RequiredArgsConstructor
public class MenuEntryComposer {
    private final LootFiltersPlugin plugin;
    public MenuEntry[] lastTickEntries = new MenuEntry[]{};

    public void onClientTick() {
        if (plugin.getClient().isMenuOpen()) {
            return;
        }

        var menu = plugin.getClient().getMenu();
        var entries = menu.getMenuEntries();
        var bounds = findBounds(List.of(entries), MenuEntryComposer::isGroundItem);
        if (bounds[0] != -1) {
            var items = Arrays.copyOfRange(entries, bounds[0], bounds[1]);
            var sorted = Arrays.stream(items).sorted((i, j) -> {
                var itemI = getItemForEntry(i);
                var itemJ = getItemForEntry(j);
                var matchI = plugin.getActiveFilter().findMatch(plugin, itemI);
                var matchJ = plugin.getActiveFilter().findMatch(plugin, itemJ);
                var sortI = matchI != null ? matchI.getSort() : 0;
                var sortJ = matchJ != null ? matchJ.getSort() : 0;
                return sortI - sortJ;
            }).collect(Collectors.toList());

            for (var i = bounds[0]; i < bounds[1]; ++i) {
                entries[i] = sorted.get(i - bounds[0]);
            }
            menu.setMenuEntries(entries);
        }

        lastTickEntries = entries;
    }

    public void onMenuEntryAdded(MenuEntry entry) { // recolor/add quantity
        if (!isGroundItem(entry)) {
            return;
        }

        var item = getItemForEntry(entry);
        var match = plugin.getActiveFilter().findMatch(plugin, item);
        if (match == null) {
            return;
        }

        entry.setDeprioritized(match.isHidden());
        entry.setTarget(buildTargetText(item, match));
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

    private String buildTargetText(TileItem item, DisplayConfig display) {
        var text = plugin.getItemName(item.getId());
        if (item.getQuantity() > 1) {
            text += " (" + item.getQuantity() + ")";
        }

        var colorTag = colorTag(display.getMenuTextColor());
        return colorTag + text;
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
