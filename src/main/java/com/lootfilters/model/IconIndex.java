package com.lootfilters.model;

import com.lootfilters.LootFiltersPlugin;
import lombok.RequiredArgsConstructor;
import net.runelite.api.WorldView;

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

    public void inc(BufferedImageProvider provider, PluginTileItem item, int... height) {
        index.compute(provider.getCacheKey(item, height), (k, entry) -> {
            if (entry == null) {
                var newEntry = new CacheEntry(provider.getImage(plugin, item, height));
                newEntry.incRef(item.getWorldView());
                return newEntry;
            }

            entry.incRef(item.getWorldView());
            return entry;
        });
    }

    public void dec(BufferedImageProvider provider, PluginTileItem item, int... height) {
        index.compute(provider.getCacheKey(item, height), (k, entry) -> {
            if (entry == null) {
                return null;
            }

            entry.decRef(item.getWorldView());
            return entry.refCount() == 0 ? null : entry;
        });
    }

    public void remove(WorldView worldView) {
        for (var entry : index.values()) {
            entry.removeRef(worldView.getId());
        }
        index.values().removeIf(it -> it.refCount() == 0);
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
                    inc(match.getIcon(), item, match.isCompact() ? plugin.getConfig().compactRenderSize() : 16);
                }
            }
        }
    }

    @RequiredArgsConstructor
    private static final class CacheEntry {
        final BufferedImage image;
        Map<Integer, Integer> refCounts = new HashMap<>(); // wv id -> count

        public void incRef(int wvId) {
            refCounts.compute(wvId, (k, count) -> count != null ? ++count : 1);
        }

        public void decRef(int wvId) {
            if (refCounts.containsKey(wvId)) {
                refCounts.compute(wvId, (k, count) -> count > 1 ? --count : null);
            }
        }

        public void removeRef(int wvId) {
            refCounts.remove(wvId);
        }

        public int refCount() {
            var total = 0;
            for (var count : refCounts.values()) {
                total += count;
            }
            return total;
        }
    }
}
