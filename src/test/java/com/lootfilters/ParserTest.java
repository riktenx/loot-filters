package com.lootfilters;

import com.lootfilters.lang.Lexer;
import com.lootfilters.lang.Parser;
import com.lootfilters.rule.Comparator;
import com.lootfilters.rule.ItemTradeableRule;
import com.lootfilters.rule.ItemValueRule;
import org.junit.Test;

import java.awt.Color;
import java.util.List;

import static com.lootfilters.TestUtil.loadTestResource;
import static org.junit.Assert.assertEquals;

public class ParserTest {
    @Test
    public void testSingleRule() throws Exception {
        var input = loadTestResource("parser-test.rs2f");

        var expectName = "loot tiers";
        var expectDesc = "loot tiers like the ground items builtin";
        var expectArea = new int[]{1,2,3,4,5,6};
        var expectMatchers = List.of(
                new MatcherConfig(new ItemValueRule(10_000_000, Comparator.GT),
                        DisplayConfig.builder()
                                .textColor(new Color(0xff,0x80,0x00, 0xff))
                                .showLootbeam(true)
                                .build()),
                new MatcherConfig(new ItemValueRule(1_000_000, Comparator.GT),
                        DisplayConfig.builder()
                                .textColor(new Color(0xa3,0x35,0xee, 0xff))
                                .showLootbeam(true)
                                .build()),
                new MatcherConfig(new ItemValueRule(100_000, Comparator.GT),
                        DisplayConfig.builder()
                                .textColor(new Color(0x00,0x70,0xdd, 0xff))
                                .build()),
                new MatcherConfig(new ItemValueRule(10_000, Comparator.GT),
                        DisplayConfig.builder()
                                .textColor(new Color(0x1e,0xff,0x00, 0xff))
                                .build()),
                new MatcherConfig(new ItemTradeableRule(false),
                        DisplayConfig.builder()
                                .textColor(new Color(0xff,0x80,0x00, 0xff))
                                .build())
        );
        var expect = new LootFilter(expectName, expectDesc, expectArea, expectMatchers);

        var tokens = new Lexer(input).tokenize();
        var parser = new Parser(tokens);
        var actual = parser.parse();
        assertEquals(expect, actual);
    }
}
