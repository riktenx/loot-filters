package com.lootfilters.lang;

import com.lootfilters.DisplayConfig;
import com.lootfilters.LootFilter;
import com.lootfilters.MatcherConfig;
import com.lootfilters.rule.AndRule;
import com.lootfilters.rule.Comparator;
import com.lootfilters.rule.ItemIdRule;
import com.lootfilters.rule.ItemNameRule;
import com.lootfilters.rule.ItemQuantityRule;
import com.lootfilters.rule.ItemValueRule;
import com.lootfilters.rule.OrRule;
import com.lootfilters.rule.Rule;
import com.lootfilters.rule.Sound;
import com.lootfilters.rule.TextAccent;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.lootfilters.lang.Token.Type.ASSIGN;
import static com.lootfilters.lang.Token.Type.BLOCK_END;
import static com.lootfilters.lang.Token.Type.BLOCK_START;
import static com.lootfilters.lang.Token.Type.COLON;
import static com.lootfilters.lang.Token.Type.COMMA;
import static com.lootfilters.lang.Token.Type.EXPR_END;
import static com.lootfilters.lang.Token.Type.EXPR_START;
import static com.lootfilters.lang.Token.Type.IDENTIFIER;
import static com.lootfilters.lang.Token.Type.IF;
import static com.lootfilters.lang.Token.Type.LIST_END;
import static com.lootfilters.lang.Token.Type.LIST_START;
import static com.lootfilters.lang.Token.Type.LITERAL_INT;
import static com.lootfilters.lang.Token.Type.LITERAL_STRING;
import static com.lootfilters.lang.Token.Type.META;
import static com.lootfilters.lang.Token.Type.OP_AND;
import static com.lootfilters.lang.Token.Type.OP_OR;
import static com.lootfilters.lang.Token.Type.STMT_END;
import static com.lootfilters.util.TextUtil.parseArgb;

// Parser somewhat mixes canonical stages 2 (parse) and 3/4 (syntax/semantic analysis) but the filter language is
// restricted enough that it should be fine for now.
public class Parser {
    private final TokenStream tokens;
    private final List<MatcherConfig> matchers = new ArrayList<>();

    private String name;
    private String description;
    private int[] activationArea = null;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    public LootFilter parse() throws ParseException {
        while (tokens.isNotEmpty()) {
            var tok = tokens.take();
            if (tok.is(META)) {
                parseMeta();
            } else if (tok.is(IF)) {
                parseMatcher();
            } else {
                throw new ParseException("unexpected token", tok);
            }
        }
        return new LootFilter(name, description, activationArea, matchers);
    }

    private void parseMeta() {
        var block = tokens.take(BLOCK_START, BLOCK_END);
        while (block.isNotEmpty()) {
            var tok = block.takeExpect(IDENTIFIER);
            block.takeExpect(ASSIGN);
            switch (tok.getValue()) {
                case "name":
                    name = block.takeExpectLiteral().expectString();
                    block.takeExpect(STMT_END);
                    break;
                case "description":
                    description = block.takeExpectLiteral().expectString();
                    block.takeExpect(STMT_END);
                    break;
                case "area":
                    block.takeExpect(LIST_START);
                    int x0 = block.takeExpectLiteral().expectInt(); block.takeExpect(COMMA);
                    int y0 = block.takeExpectLiteral().expectInt(); block.takeExpect(COMMA);
                    int z0 = block.takeExpectLiteral().expectInt(); block.takeExpect(COMMA);
                    int x1 = block.takeExpectLiteral().expectInt(); block.takeExpect(COMMA);
                    int y1 = block.takeExpectLiteral().expectInt(); block.takeExpect(COMMA);
                    int z1 = block.takeExpectLiteral().expectInt(); block.takeOptional(COMMA);
                    block.takeExpect(LIST_END);
                    block.takeExpect(STMT_END);

                    activationArea = new int[]{x0,y0,z0,x1,y1,z1};
                    break;
                default:
                    throw new ParseException("unrecognized metavalue", tok);
            }
        }
    }

    private void parseMatcher() {
        var operators = new Stack<Token>();
        var rulesPostfix = new ArrayList<Rule>();
        tokens.walkExpression(EXPR_START, EXPR_END, it -> {
            if (it.is(EXPR_START)) {
                operators.push(it);
            } else if (it.is(EXPR_END)) {
                while (!operators.isEmpty() && !operators.peek().is(EXPR_START)) {
                    var op = operators.pop();
                    if (op.is(OP_AND)) {
                        rulesPostfix.add(new AndRule(null));
                    } else if (op.is(OP_OR)) {
                        rulesPostfix.add(new OrRule(null));
                    }
                }
            } else if (it.is(OP_AND)) {
                operators.push(it);
            } else if (it.is(OP_OR)) {
                while (!operators.isEmpty() && operators.peek().is(OP_AND)) {
                    operators.pop();
                    rulesPostfix.add(new AndRule(null));
                }
                operators.push(it);
            } else if (it.is(IDENTIFIER)) {
                rulesPostfix.add(parseRule(it));
            } else {
                throw new ParseException("unexpected token in expression", it);
            }
        });

        while (!operators.isEmpty()) { // is this necessary? since parenthesis around overall expr are guaranteed
            var op = operators.pop();
            if (op.is(OP_AND)) {
                rulesPostfix.add(new AndRule(null));
            } else if (op.is(OP_OR)) {
                rulesPostfix.add(new OrRule(null));
            }
        }

        // rule expression MUST be followed by block w/ display config assignments
        tokens.takeExpect(BLOCK_START);
        var builder = DisplayConfig.builder();
        while (!tokens.peek().is(BLOCK_END)) { // TokenStream.traverseBlock?
            var assign = parseAssignment();
            switch (assign[0].getValue()) {
                case "textColor":
                case "color":
                    builder.textColor(parseArgb(assign[1].expectString())); break;
                case "backgroundColor":
                    builder.backgroundColor(parseArgb(assign[1].expectString())); break;
                case "borderColor":
                    builder.borderColor(parseArgb(assign[1].expectString())); break;
                case "hidden":
                    builder.hidden(assign[1].expectBoolean()); break;
                case "showLootbeam":
                    builder.showLootbeam(assign[1].expectBoolean()); break;
                case "showValue":
                    builder.showValue(assign[1].expectBoolean()); break;
                case "showDespawn":
                    builder.showDespawn(assign[1].expectBoolean()); break;
                case "notify":
                    builder.notify(assign[1].expectBoolean()); break;
                case "textAccent":
                    builder.textAccent(TextAccent.fromOrdinal(assign[1].expectInt())); break;
                case "sound":
                    builder.sound(new Sound.File(assign[1].expectString())); break;
                case "textAccentColor":
                    builder.textAccentColor(parseArgb(assign[1].expectString())); break;
                default:
                    throw new ParseException("unexpected identifier in display config block", assign[0]);
            }
        }
        tokens.takeExpect(BLOCK_END);

        matchers.add(new MatcherConfig(buildRule(rulesPostfix), builder.build()));
    }

    private Rule parseRule(Token first) {
        tokens.takeExpect(COLON); // grammar is always <id><colon><...>
        switch (first.getValue()) {
            case "id":
                return parseItemIdRule();
            case "name":
                return parseItemNameRule();
            case "quantity":
                return parseItemQuantityRule();
            case "value":
                return parseItemValueRule();
            default:
                throw new ParseException("unknown rule identifier", first);
        }
    }

    private ItemIdRule parseItemIdRule() {
        var id = tokens.takeExpect(LITERAL_INT);
        return new ItemIdRule(Integer.parseInt(id.getValue()));
    }

    private ItemNameRule parseItemNameRule() {
        var name = tokens.takeExpect(LITERAL_STRING);
        return new ItemNameRule(name.getValue());
    }

    private ItemQuantityRule parseItemQuantityRule() {
        var op = tokens.take();
        var value = tokens.takeExpect(LITERAL_INT);
        return new ItemQuantityRule(Integer.parseInt(value.getValue()), Comparator.fromToken(op));
    }

    private ItemValueRule parseItemValueRule() {
        var op = tokens.take();
        var value = tokens.takeExpect(LITERAL_INT);
        return new ItemValueRule(Integer.parseInt(value.getValue()), Comparator.fromToken(op));
    }

    private Rule buildRule(List<Rule> postfix) {
        var operands = new Stack<Rule>();
        for (var rule : postfix) {
            if (rule instanceof ItemIdRule
                    || rule instanceof ItemNameRule
                    || rule instanceof ItemQuantityRule
                    || rule instanceof ItemValueRule) {
                operands.push(rule);
            } else if (rule instanceof AndRule) {
                operands.push(new AndRule(operands.pop(), operands.pop()));
            } else if (rule instanceof OrRule) {
                operands.push(new OrRule(operands.pop(), operands.pop()));
            }
        }

        if (operands.size() != 1) {
            throw new ParseException("invalid rule postfix");
        }
        return operands.pop();
    }

    private Token[] parseAssignment() { // assignments do not support nested expressions, making this trivial
        var ident = tokens.takeExpect(IDENTIFIER);
        tokens.takeExpect(ASSIGN);
        var value = tokens.takeExpectLiteral();
        tokens.takeExpect(STMT_END);
        return new Token[]{ident, value};
    }
}
