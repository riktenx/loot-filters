package com.lootfilters;

import com.lootfilters.model.PluginTileItem;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class LootbeamIndex {
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
        beam.setActive(true);
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
        beam.setActive(false);
        beams.remove(item);
        if (beams.isEmpty()) {
            index.remove(tile);
        }
    }

    public void remove(WorldView worldView) {
        for (var tile : index.keySet()) {
            if (tile.getItemLayer().getWorldView().getId() == worldView.getId()) {
                var beams = index.get(tile);
                for (var beam : beams.values()) {
                    beam.setActive(false);
                }
            }
        }
        index.keySet().removeIf(it -> it.getItemLayer().getWorldView().getId() == worldView.getId());
    }

    public void clear() {
        for (var beams : index.values()) {
            for (var beam : beams.values()) {
                beam.setActive(false);
            }
        }
        index.clear();
    }

    public void reset(LootFiltersPlugin plugin) {
        clear();
        for (var entry : plugin.getTileItemIndex().entrySet()) {
            var tile = entry.getKey();
            for (var item : entry.getValue()) {
                var match = plugin.getActiveFilter().findMatch(plugin, item);
                if (match.isShowLootbeam()) {
                    put(tile, item, new Lootbeam(plugin.getConfig(), plugin.getClient(), plugin.getClientThread(), tile.getLocalLocation(),
                            match.getLootbeamColor()));
                }
            }
        }
    }
}
