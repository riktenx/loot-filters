package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.runelite.api.TileItem;

@EqualsAndHashCode(callSuper = false)
@ToString
public class NotRule extends Rule {
    private final Rule inner;

    public NotRule(Rule inner) {
        super("not");

        this.inner = inner;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        return !inner.test(plugin, item);
    }
}
