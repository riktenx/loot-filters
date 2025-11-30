package com.lootfilters;

import com.lootfilters.model.PluginTileItem;
import lombok.AllArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.coords.WorldPoint;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lootfilters.util.CollectionUtil.findBounds;
import static com.lootfilters.util.TextUtil.abbreviate;
import static net.runelite.client.util.ColorUtil.colorTag;
import static net.runelite.client.util.ColorUtil.wrapWithColorTag;

@AllArgsConstructor
public class MenuEntryComposer {
    private final LootFiltersPlugin plugin;

    public void onMenuEntryAdded(MenuEntry entry) { // recolor/add quantity
        if (!isGroundItem(entry)) {
            return;
        }

        var items = getItemsForEntry(entry);
        if (isIndeterminate(items)) {
            var quantity = items.stream().mapToInt(PluginTileItem::getQuantity).sum();
            var deprioritized = plugin.getConfig().deprioritizeHidden() && items.stream().allMatch(this::isHidden);
            entry.setTarget(entry.getTarget() + " (" + quantity + ")");
            entry.setDeprioritized(deprioritized);
            return;
        }

        var item = items.get(0);
        var match = plugin.getDisplayIndex().get(item);

        entry.setDeprioritized(plugin.getConfig().deprioritizeHidden() && match.isHidden());
        var color = match.isHidden() && plugin.getConfig().recolorHidden()
                ? plugin.getConfig().hiddenColor()
                : match.getMenuTextColor();
        entry.setTarget(buildTargetText(item, entry.getTarget(), color));
    }

    public void onClientTick() { // sort -> collapse
        if (plugin.getClient().isMenuOpen()) {
            return;
        }

        var entries = sortEntries(plugin.getClient().getMenu().getMenuEntries());
        if (plugin.getConfig().collapseEntries()) {
            entries = collapseEntries(entries);
        }
        if (plugin.getConfig().showAnalyzer()) {
            entries = addAnalyzers(entries);
        }
        plugin.getClient().getMenu().setMenuEntries(entries);
    }

    private MenuEntry[] sortEntries(MenuEntry[] entries) {
        var bounds = findBounds(List.of(entries), MenuEntryComposer::isGroundItem);
        if (bounds[0] == -1) { // no items to sort
            return entries;
        }

        var items = Arrays.copyOfRange(entries, bounds[0], bounds[1]);
        var sortedItems = Arrays.stream(items).sorted((i, j) -> {
            var itemsI = getItemsForEntry(i);
            var itemsJ = getItemsForEntry(j);
            var displayI = isIndeterminate(itemsI) ? null : plugin.getDisplayIndex().get(itemsI.get(0));
            var displayJ = isIndeterminate(itemsJ) ? null : plugin.getDisplayIndex().get(itemsJ.get(0));
            var sortI = displayI != null ? displayI.getMenuSort() : 0;
            var sortJ = displayJ != null ? displayJ.getMenuSort() : 0;
            return sortI - sortJ;
        }).toArray(MenuEntry[]::new);

        var sorted = entries.clone();
        for (var i = bounds[0]; i < bounds[1]; ++i) {
            sorted[i] = sortedItems[i - bounds[0]];
        }
        return sorted;
    }

    private MenuEntry[] collapseEntries(MenuEntry[] entries) {
        var itemCounts = Stream.of(entries)
                .filter(MenuEntryComposer::isGroundItem)
                .collect(Collectors.groupingBy(MenuEntryComposer::entrySlug, Collectors.counting()));

        // the displayed list is built IN REVERSE of the actual array contents - so you have to collapse in reverse as
        // well, otherwise you will break the original order
        // this is accomplished trivially by reverse -> collapse -> reverse
        var reversed = Arrays.asList(entries);
        Collections.reverse(reversed);

        var collapsed = reversed.stream()
                .map(it -> isGroundItem(it)
                        ? withCount(it, itemCounts.getOrDefault(entrySlug(it), 1L))
                        : it)
                .distinct()
                .collect(Collectors.toList());
        Collections.reverse(collapsed);

        return collapsed.toArray(MenuEntry[]::new);
    }

    private MenuEntry[] addAnalyzers(MenuEntry[] entries) {
        return Arrays.stream(entries)
                .flatMap(it -> isGroundItem(it, false)
                        ? Stream.of(it, getAnalyzer(it))
                        : Stream.of(it))
                .toArray(MenuEntry[]::new);
    }

    private MenuEntry getAnalyzer(MenuEntry entry) {
        var item = getItemsForEntry(entry).get(0);
        var display = plugin.getDisplayIndex().get(item);
        Consumer<MenuEntry> onClick = (e) -> {
            var trace = display.getEvalTrace();
            if (trace.isEmpty()) {
                plugin.addChatMessage(item.getName() + " did not match any config list or filter rule.");
            } else if (trace.size() == 1 && trace.get(0) == -4) {
                plugin.addChatMessage(item.getName() + " is hidden by the 'Item lists' -> 'Hidden items' setting.");
            } else if (trace.size() == 1 && trace.get(0) == -3) {
                plugin.addChatMessage(item.getName() + " is highlighted by the 'Item lists' -> 'Highlighted items' setting.");
            } else if (trace.size() == 1 && trace.get(0) == -2) {
                plugin.addChatMessage(item.getName() + " is hidden by the 'General' -> 'Item spawn filter' setting.");
            } else if (trace.size() == 1 && trace.get(0) == -1) {
                plugin.addChatMessage(item.getName() + " is hidden by the 'General' -> 'Ownership filter' setting.");
            } else {
                plugin.addChatMessage(item.getName() + " matched lines " + trace);
            }
        };

        return plugin.getClient().getMenu().createMenuEntry(-1)
                .setOption("[Loot Filters]: Analyze")
                .setTarget(entry.getTarget())
                .setType(MenuAction.RUNELITE)
                .onClick(onClick);
    }

    private MenuEntry withCount(MenuEntry entry, long count) {
        return count > 1
                ? entry.setTarget(entry.getTarget() + " x" + count)
                : entry;
    }

    private List<PluginTileItem> getItemsForEntry(MenuEntry entry) {
        var wv = plugin.getClient().getWorldView(entry.getWorldViewId());
        var point = WorldPoint.fromScene(wv, entry.getParam0(), entry.getParam1(), wv.getPlane());
        return plugin.getTileItemIndex().findItem(point, entry.getIdentifier());
    }

    private String buildTargetText(PluginTileItem item, String baseTarget, Color color) {
        var start = baseTarget.lastIndexOf('>'); // check for WIDGET_TARGET_ON_GROUND_ITEM

        var text = baseTarget.substring(start > -1 ? start + 1 : 0);
        if (item.getQuantity() > 1) {
            text += " (" + abbreviate(item.getQuantity()) + ")";
        }

        var prefix = start > -1 ? baseTarget.substring(0, start + 1) : "";
        return prefix + wrapWithColorTag(text, color);
    }

    private static boolean isGroundItem(MenuEntry entry, boolean includeNonOptions) {
        var type = entry.getType();
        return type == MenuAction.GROUND_ITEM_FIRST_OPTION
                || type == MenuAction.GROUND_ITEM_SECOND_OPTION
                || type == MenuAction.GROUND_ITEM_THIRD_OPTION
                || type == MenuAction.GROUND_ITEM_FOURTH_OPTION
                || type == MenuAction.GROUND_ITEM_FIFTH_OPTION
                || includeNonOptions && type == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM
                || includeNonOptions && type == MenuAction.EXAMINE_ITEM_GROUND;
    }

    private static boolean isGroundItem(MenuEntry entry) {
        return isGroundItem(entry, true);
    }

    // The results of a menu entry lookup are "indeterminate" if the item was stackable, and we found more than one.
    // This limits what we can do in menu entry ops because we do not know what each menu entry actually points to
    // when clicked (e.g. we cannot reliably display the quantity that will be taken).
    private static boolean isIndeterminate(List<PluginTileItem> items) {
        return items.isEmpty() || items.size() > 1 && items.get(0).isStackable();
    }

    private static String entrySlug(MenuEntry entry) {
        return entry.getType().toString() + entry.getIdentifier() + entry.getParam0() + entry.getParam1();
    }

    private boolean isHidden(PluginTileItem item) {
        var display = plugin.getDisplayIndex().get(item);
        return display != null && display.isHidden();
    }
}
