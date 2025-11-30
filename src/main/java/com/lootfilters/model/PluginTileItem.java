package com.lootfilters.model;

import com.lootfilters.LootFiltersPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

import java.time.Instant;

public class PluginTileItem {
    private final TileItem item;
    @Getter private final String name;
    private final int gePrice;
    @Getter private final int haPrice;
    @Getter private final WorldPoint worldPoint;
    @Getter private final int spawnTime;
    @Getter private final Instant despawnInstant;
    @Getter private final boolean isStackable;
    @Getter private final boolean isNoted;
    @Getter private final boolean isTradeable;
    @Getter private final Tile tile;
    @Getter private final int worldView;

    @Setter
    private int quantityOverride = -1;

    public PluginTileItem(LootFiltersPlugin plugin, Tile tile, TileItem item) {
        var composition = plugin.getItemManager().getItemComposition(item.getId());
        var linkedNoteComposition = plugin.getItemManager().getItemComposition(composition.getLinkedNoteId());

        this.item = item;
        this.name = composition.getName();
        this.gePrice = plugin.getItemManager().getItemPrice(item.getId());
        this.haPrice = composition.getHaPrice();
        this.worldPoint = WorldPoint.fromLocalInstance(plugin.getClient(), tile.getLocalLocation());
        this.spawnTime = plugin.getClient().getTickCount();
        this.despawnInstant = Instant.now().plusMillis((getDespawnTime() - spawnTime) * 600L);
        this.isStackable = composition.isStackable();
        this.isNoted = composition.getNote() != -1;
        this.isTradeable = composition.isTradeable() || linkedNoteComposition.isTradeable();
        this.tile = tile;
        this.worldView = tile.getItemLayer().getWorldView().getId();
    }

    public int getGePrice() {
        switch (getId()) {
            case ItemID.COINS:
                return 1;
            case ItemID.PLATINUM:
                return 1000;
            default:
                return gePrice;
        }
    }

    public boolean isMoney() {
        return getId() == ItemID.COINS || getId() == ItemID.PLATINUM;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PluginTileItem && ((PluginTileItem) other).item == item
                || other instanceof TileItem && other == item;
    }

    @Override
    public int hashCode() {
        return item.hashCode();
    }

    public int getQuantity() {
        return quantityOverride > -1 ? quantityOverride : item.getQuantity();
    }

    public int getId() { return item.getId(); }
    public int getVisibleTime() { return item.getVisibleTime(); }
    public int getDespawnTime() { return item.getDespawnTime(); }
    public int getOwnership() { return item.getOwnership(); }
    public boolean isPrivate() { return item.isPrivate(); }
}
