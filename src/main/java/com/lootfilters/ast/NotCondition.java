package com.lootfilters.ast;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class NotCondition extends Condition {
    private final Condition inner;

    public NotCondition(Condition inner) {
        this.inner = inner;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return !inner.test(plugin, item);
    }
}
