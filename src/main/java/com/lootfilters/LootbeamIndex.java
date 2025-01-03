package com.lootfilters;

import net.runelite.api.Tile;
import net.runelite.api.TileItem;

import java.util.HashMap;
import java.util.Map;

public class LootbeamIndex {
    private final Map<Tile, Map<TileItem, Lootbeam>> index = new HashMap<>();

    public void put(Tile tile, TileItem item, Lootbeam beam) {
        if (!index.containsKey(tile)) {
            index.put(tile, new HashMap<>());
        }

        var beams = index.get(tile);
        beams.put(item, beam);
    }

    public void remove(Tile tile, TileItem item) {
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
}
