package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import net.runelite.api.gameval.VarbitID;

public class AccountTypeRule extends LeafRule {
    private final int type;

    public AccountTypeRule(int type) {
        super();
        this.type = type;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return plugin.getClient().getVarbitValue(VarbitID.IRONMAN) == type;
    }
}
