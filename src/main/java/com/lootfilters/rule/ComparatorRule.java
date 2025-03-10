package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.ToString;
import net.runelite.api.TileItem;

@ToString
public abstract class ComparatorRule extends Rule {
    private final int rhs;
    private final Comparator cmp;

    protected ComparatorRule(String discriminator, int rhs, Comparator cmp) {
        super(discriminator);
        this.rhs = rhs;
        this.cmp = cmp;
    }

    @Override
    public final boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        var lhs = getLhs(plugin, item);
        switch (cmp) {
            case GT:
                return lhs > rhs;
            case LT:
                return lhs < rhs;
            case EQ:
                return lhs == rhs;
            case GT_EQ:
                return lhs >= rhs;
            case LT_EQ:
                return lhs <= rhs;
        }
        return false;
    }

    public abstract int getLhs(LootFiltersPlugin plugin, TileItem item);
}
