package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.Comparator;
import com.lootfilters.ast.LeafCondition;
import com.lootfilters.model.PluginTileItem;
import lombok.ToString;

@ToString
public abstract class ComparatorCondition extends LeafCondition {
    private final int rhs;
    private final Comparator cmp;

    protected ComparatorCondition(int rhs, Comparator cmp) {
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

    public abstract int getLhs(LootFiltersPlugin plugin, PluginTileItem item);
}
