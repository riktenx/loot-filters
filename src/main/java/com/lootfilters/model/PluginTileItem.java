package com.lootfilters.model;

import com.lootfilters.LootFiltersPlugin;
import lombok.Getter;
import net.runelite.api.Model;
import net.runelite.api.Node;
import net.runelite.api.TileItem;

import java.time.Instant;

public class PluginTileItem implements TileItem {
    private final TileItem item;
    @Getter private final int spawnTime;
    @Getter private final Instant despawnInstant;

    public PluginTileItem(LootFiltersPlugin plugin, TileItem item) {
        this.item = item;
        this.spawnTime = plugin.getClient().getTickCount();
        this.despawnInstant = Instant.now().plusMillis((getDespawnTime() - spawnTime) * 600L);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PluginTileItem && ((PluginTileItem) other).item == item;
    }

    @Override
    public int hashCode() {
        return item.hashCode();
    }

    @Override public int getId() { return item.getId(); }
    @Override public int getQuantity() { return item.getQuantity(); }
    @Override public int getVisibleTime() { return item.getVisibleTime(); }
    @Override public int getDespawnTime() { return item.getDespawnTime(); }
    @Override public int getOwnership() { return item.getOwnership(); }
    @Override public boolean isPrivate() { return item.isPrivate(); }
    @Override public Model getModel() { return item.getModel(); }
    @Override public int getModelHeight() { return item.getModelHeight(); }
    @Override public void setModelHeight(int i) { item.setModelHeight(i); }
    @Override public Node getNext() { return item.getNext(); }
    @Override public Node getPrevious() { return item.getPrevious(); }
    @Override public long getHash() { return item.getHash(); }
}
