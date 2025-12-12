package com.lootfilters.model;

import com.lootfilters.DisplayConfig;
import com.lootfilters.LootFiltersPlugin;
import lombok.NonNull;
import net.runelite.api.WorldView;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DisplayConfigIndex {
    private final Map<PluginTileItem, DisplayConfig> index = new HashMap<>();

    public @NonNull DisplayConfig get(PluginTileItem item) {
        return index.get(item);
    }

    public void put(PluginTileItem item, @NonNull DisplayConfig display) {
        index.put(item, display);
    }

    public void remove(PluginTileItem item) {
        index.remove(item);
    }

    public void remove(WorldView worldView) {
        index.keySet().removeIf(it -> it.getWorldView() == worldView.getId());
    }

    public int size() {
        return index.size();
    }

    public void clear() {
        index.clear();
    }

    public void reset(LootFiltersPlugin plugin) {
        clear();
        for (var entry : plugin.getTileItemIndex().entrySet()) {
            for (var item : entry.getValue()) {
                var match = plugin.getActiveFilter().findMatch(plugin, item);
                index.put(item, match);
            }
        }
    }
}
