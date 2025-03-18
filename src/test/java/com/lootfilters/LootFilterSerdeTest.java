package com.lootfilters;

import com.google.gson.Gson;
import com.lootfilters.rule.AndRule;
import com.lootfilters.rule.Comparator;
import com.lootfilters.rule.ItemIdRule;
import com.lootfilters.rule.ItemNameRule;
import com.lootfilters.rule.ItemQuantityRule;
import com.lootfilters.rule.ItemValueRule;
import com.lootfilters.rule.OrRule;
import com.lootfilters.rule.ValueType;
import org.junit.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LootFilterSerdeTest {
    @Test
    public void testSerde() {
        var filter = new LootFilter(
                "foo",
                "bar",
                null,
                List.of(
                        new MatcherConfig(new ItemIdRule(1), new DisplayConfig(Color.RED)),
                        new MatcherConfig(new ItemNameRule("bandos-crossbow"), new DisplayConfig(Color.GREEN)),
                        new MatcherConfig(new ItemValueRule(1_000, Comparator.LT, ValueType.HIGHEST), new DisplayConfig(Color.BLUE)),
                        new MatcherConfig(new ItemQuantityRule(1_000, Comparator.LT), new DisplayConfig(Color.WHITE)),
                        new MatcherConfig(
                                new AndRule(List.of(
                                        new ItemNameRule("Coins"),
                                        new ItemQuantityRule(5, Comparator.EQ)
                                )),
                                new DisplayConfig(Color.WHITE.darker())
                        ),
                        new MatcherConfig(
                                new OrRule(List.of(
                                        new ItemNameRule("Coins"),
                                        new ItemQuantityRule(9, Comparator.EQ)
                                )),
                                new DisplayConfig(Color.WHITE.darker())
                        )
                )
        );

        var gson = new Gson();
        var ser = filter.toJson(gson);
        var deser = LootFilter.fromJson(gson, ser);

        assertEquals(filter, deser);
    }
}
