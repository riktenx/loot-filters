package com.lootfilters.model;

import com.lootfilters.LootFiltersPlugin;
import lombok.RequiredArgsConstructor;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class IconIndex {
    private final LootFiltersPlugin plugin;
    private final Map<BufferedImageProvider.CacheKey, CacheEntry> index = new HashMap<>();

    public BufferedImage get(BufferedImageProvider.CacheKey key) {
        return index.containsKey(key) ? index.get(key).image : null;
    }

    public void inc(BufferedImageProvider provider, PluginTileItem item) {
        index.compute(provider.getCacheKey(item), (k, entry) -> {
            if (entry == null) {
                return new CacheEntry(provider.getImage(plugin, item));
            }

            ++entry.refCount;
            return entry;
        });
    }

    public void dec(BufferedImageProvider provider, PluginTileItem item) {
        index.compute(provider.getCacheKey(item), (k, entry) -> {
            if (entry == null) {
                return null;
            }

            --entry.refCount;
            return entry.refCount == 0 ? null : entry;
        });
    }

    public int size() {
        return index.size();
    }

    public void clear() {
        index.clear();
    }

    public void reset() {
        clear();
        for (var entry : plugin.getTileItemIndex().entrySet()) {
            for (var item : entry.getValue()) {
                var match = plugin.getActiveFilter().findMatch(plugin, item);
                if (match != null && match.getIcon() != null) {
                    inc(match.getIcon(), item);
                }
            }
        }
    }

    @RequiredArgsConstructor
    private static final class CacheEntry {
        final BufferedImage image;
        int refCount = 1;
    }
}
