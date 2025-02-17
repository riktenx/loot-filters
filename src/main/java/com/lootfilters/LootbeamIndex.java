package com.lootfilters;

import com.lootfilters.model.PluginTileItem;
import lombok.AllArgsConstructor;
import net.runelite.api.Tile;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class LootbeamIndex {
    private final LootFiltersPlugin plugin;

    private final Map<Tile, Map<PluginTileItem, Lootbeam>> index = new HashMap<>();

    public int size() {
        return index.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    public void put(Tile tile, PluginTileItem item, Lootbeam beam) {
        if (!index.containsKey(tile)) {
            index.put(tile, new HashMap<>());
        }

        var beams = index.get(tile);
        beams.put(item, beam);
    }

    public void remove(Tile tile, PluginTileItem item) {
        if (!index.containsKey(tile)) {
            return; // what?
        }

        var beams = index.get(tile);
        if (!beams.containsKey(item)) {
            return; // what?
        }

        var beam = beams.get(item);
        beam.remove();
        beams.remove(item);
        if (beams.isEmpty()) {
            index.remove(tile);
        }
    }

    public void clear() {
        for (var beams : index.values()) {
            for (var beam : beams.values()) {
                beam.remove();
            }
        }
        index.clear();
    }

    public void reset() {
        clear();
        for (var entry : plugin.getTileItemIndex().entrySet()) {
            var tile = entry.getKey();
            for (var item : entry.getValue()) {
                var match = plugin.getActiveFilter().findMatch(plugin, item);
                if (match != null && match.isShowLootbeam()) {
                    put(tile, item, new Lootbeam(plugin.getClient(), plugin.getClientThread(), tile.getWorldLocation(),
                            match.getLootbeamColor(), Lootbeam.Style.MODERN));
                }
            }
        }
    }
}
