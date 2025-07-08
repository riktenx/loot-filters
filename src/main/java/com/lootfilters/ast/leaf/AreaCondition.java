package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.ast.LeafCondition;
import com.lootfilters.model.PluginTileItem;
import net.runelite.api.coords.WorldPoint;

public class AreaCondition extends LeafCondition {
    private final WorldPoint p0, p1;

    public AreaCondition(WorldPoint p0, WorldPoint p1) {
        this.p0 = p0;
        this.p1 = p1;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        var p = item.getWorldPoint();
        return p.getX() >= p0.getX() && p.getY() >= p0.getY() && p.getPlane() >= p0.getPlane()
                && p.getX() <= p1.getX() && p.getY() <= p1.getY() && p.getPlane() <= p1.getPlane();
    }
}
