package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.ast.LeafCondition;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ConstCondition extends LeafCondition {
    private final boolean target;

    public ConstCondition(boolean target) {
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return target;
    }
}
