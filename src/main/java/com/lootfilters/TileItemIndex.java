package com.lootfilters;

import com.lootfilters.model.PluginTileItem;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TileItemIndex {
    private final Map<Tile, List<PluginTileItem>> itemIndex = new HashMap<>();

    // Tile instances are not readily available in all contexts,
    private final Map<WorldPoint, Tile> pointIndex = new HashMap<>();

    public Set<Map.Entry<Tile, List<PluginTileItem>>> entrySet() {
        return itemIndex.entrySet();
    }

    public TileItem findItem(Tile tile, int id) {
        if (!itemIndex.containsKey(tile)) {
            return null;
        }

        return itemIndex.get(tile).stream()
                .filter(it -> it.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public TileItem findItem(WorldPoint point, int id) {
        return pointIndex.containsKey(point)
                ? findItem(pointIndex.get(point), id)
                : null;
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

    public void clear() {
        itemIndex.clear();
    }
}
