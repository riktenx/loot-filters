package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.ast.LeafCondition;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.model.VarTransform;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VarCondition extends LeafCondition {
    private final int varId;
    private final VarTransform transform;
    private final boolean useVarbit;

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        int value = useVarbit
                ? plugin.getClient().getVarbitValue(varId)
                : plugin.getClient().getVarpValue(varId);
        return transform.apply(value) != 0;
    }
}
