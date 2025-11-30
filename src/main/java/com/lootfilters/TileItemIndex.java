package com.lootfilters;

import com.lootfilters.model.PluginTileItem;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TileItemIndex {
    private final Map<Tile, List<PluginTileItem>> itemIndex = new HashMap<>();
    private final Map<WorldPoint, Tile> pointIndex = new HashMap<>();

    public Set<Map.Entry<Tile, List<PluginTileItem>>> entrySet() {
        return itemIndex.entrySet();
    }

    public PluginTileItem findItem(TileItem item) {
        for (var entry : itemIndex.entrySet()) {
            for (var pItem : entry.getValue()) {
                if (pItem.equals(item)) {
                    return pItem;
                }
            }
        }
        return null;
    }

    public List<PluginTileItem> findItem(Tile tile, int id) {
        if (!itemIndex.containsKey(tile)) {
            return null;
        }

        return itemIndex.get(tile).stream()
                .filter(it -> it.getId() == id)
                .collect(Collectors.toList());
    }

    public List<PluginTileItem> findItem(WorldPoint point, int id) {
        return pointIndex.containsKey(point)
                ? findItem(pointIndex.get(point), id)
                : List.of();
    }

    public void put(Tile tile, PluginTileItem item) {
        if (!itemIndex.containsKey(tile)) {
            itemIndex.put(tile, new ArrayList<>());
        }
        itemIndex.get(tile).add(item);
        pointIndex.put(tile.getWorldLocation(), tile);
    }

    public void remove(Tile tile, PluginTileItem item) {
        if (!itemIndex.containsKey(tile)) {
            return; // what?
        }

        var items = itemIndex.get(tile);
        items.remove(item);
        if (items.isEmpty()) {
            itemIndex.remove(tile);
            pointIndex.remove(tile.getWorldLocation());
        }
    }

    public void remove(WorldView worldView) {
        itemIndex.keySet().removeIf(it -> it.getItemLayer().getWorldView().getId() == worldView.getId());
        pointIndex.values().removeIf(it -> it.getItemLayer().getWorldView().getId() == worldView.getId());
    }

    public int pointIndexSize() {
        return pointIndex.size();
    }

    public void clear() {
        itemIndex.clear();
        pointIndex.clear();
    }
}
