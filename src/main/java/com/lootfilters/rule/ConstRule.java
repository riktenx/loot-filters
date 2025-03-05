package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.runelite.api.TileItem;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ConstRule extends Rule {
    private final boolean target;

    public ConstRule(boolean target) {
        super("const");
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        return target;
    }
}
