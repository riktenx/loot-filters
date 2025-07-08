package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.ast.LeafCondition;
import com.lootfilters.model.PluginTileItem;
import net.runelite.api.gameval.VarbitID;

public class AccountTypeCondition extends LeafCondition {
    private final int type;

    public AccountTypeCondition(int type) {
        this.type = type;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return plugin.getClient().getVarbitValue(VarbitID.IRONMAN) == type;
    }
}
