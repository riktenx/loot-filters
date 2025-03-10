package com.lootfilters;

import com.lootfilters.lang.Lexer;
import com.lootfilters.lang.Parser;
import com.lootfilters.rule.AndRule;
import com.lootfilters.rule.Comparator;
import com.lootfilters.rule.ItemNameRule;
import com.lootfilters.rule.ItemNotedRule;
import com.lootfilters.rule.ItemStackableRule;
import com.lootfilters.rule.ItemTradeableRule;
import com.lootfilters.rule.ItemValueRule;
import com.lootfilters.rule.NotRule;
import com.lootfilters.rule.OrRule;
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
                                .build()),
                new MatcherConfig(new ItemStackableRule(false),
                        DisplayConfig.builder()
                                .textColor(new Color(0xff,0x90,0x00, 0xff))
                                .build()),
                new MatcherConfig(new ItemNotedRule(false),
                        DisplayConfig.builder()
                                .textColor(new Color(0xff,0x95,0x00, 0xff))
                                .build()),
                new MatcherConfig(new NotRule(new ItemNameRule("foo")),
                        DisplayConfig.builder().hidden(true).build()),
                new MatcherConfig(new NotRule(new ItemNameRule("foo")),
                        DisplayConfig.builder().hidden(true).build()),
                new MatcherConfig(new OrRule(new ItemNameRule("bar"), new NotRule(new ItemNameRule("foo"))),
                        DisplayConfig.builder().hidden(true).build()),
                new MatcherConfig(
                        new AndRule(
                                new ItemNameRule("foo"),
                                new NotRule(
                                        new AndRule(
                                                new NotRule(new ItemNameRule("baz")),
                                                new ItemNameRule("bar")
                                        )
                                )
                        ),
                        DisplayConfig.builder().hidden(true).build())
        );
        var expect = new LootFilter(expectName, expectDesc, expectArea, expectMatchers);

        var lexedTokens = new Lexer("ParserTest", input).tokenize();
        var parser = new Parser(lexedTokens);
        var actual = parser.parse();
        assertEquals(expect, actual);
    }
}
