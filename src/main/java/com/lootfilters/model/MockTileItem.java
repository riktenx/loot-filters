package com.lootfilters.model;

import net.runelite.api.Model;
import net.runelite.api.Node;
import net.runelite.api.TileItem;

public class MockTileItem implements TileItem {
    @Override public int getId() { return 0; }
    @Override public int getQuantity() { return 1; }
    @Override public int getVisibleTime() { return 0; }
    @Override public int getDespawnTime() { return 0; }
    @Override public int getOwnership() { return 0; }
    @Override public boolean isPrivate() { return false; }
    @Override public Model getModel() { return null; }
    @Override public int getModelHeight() { return 0; }
    @Override public void setModelHeight(int modelHeight) { }
    @Override public Node getNext() { return null; }
    @Override public Node getPrevious() { return null; }
    @Override public long getHash() { return 0; }
}
