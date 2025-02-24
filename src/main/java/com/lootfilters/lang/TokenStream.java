package com.lootfilters.lang;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * TokenStream wraps a list of Tokens to expose retrieval APIs suitable for parsing.
 */
@AllArgsConstructor
public class TokenStream {
    private final List<Token> tokens;

    /**
     * Returns a shallow copy of the token stream.
     */
    public List<Token> getTokens() {
        return new ArrayList<>(tokens);
    }

    /**
     * Peek at the first token in the stream, ignoring whitespace, without consuming it.
     */
    public Token peek() {
        return tokens.stream()
                .filter(Token::isSemantic)
                .findFirst()
                .orElse(null);
    }

    /**
     * Consume the first token in the stream, optionally including whitespace.
     */
    public Token take(boolean includeWhitespace) {
        while (isNotEmpty()) {
            var next = tokens.remove(0);
            if (next.isSemantic() || includeWhitespace && next.isWhitespace()) {
                return next;
            }
        }
        return null;
    }

    /**
     * Consume the first non-whitespace token in the stream.
     */
    public Token take() {
        return take(false);
    }

    /**
     * Consume the first token in the stream, optionally including whitespace, while asserting that it is of the given
     * type.
     */
    public Token takeExpect(Token.Type expect, boolean includeWhitespace) {
        if (tokens.isEmpty()) {
            throw new ParseException("unexpected end of token stream");
        }

        var first = take(includeWhitespace);
        if (!first.is(expect)) {
            throw new ParseException("unexpected non-" + expect + " token", first);
        }
        return first;
    }

    /**
     * Consume the first non-whitespace token in the stream while asserting that it is of the given type.
     */
    public Token takeExpect(Token.Type expect) {
        return takeExpect(expect, false);
    }

    /**
     * Consumes the first non-whitespace token at the head of the stream, asserting that it is any one of the literal
     * types (int, string, boolean).
     */
    public Token takeExpectLiteral() {
        var first = take();
        if (!first.is(Token.Type.LITERAL_INT)
                && !first.is(Token.Type.LITERAL_STRING)
                && !first.is(Token.Type.TRUE)
                && !first.is(Token.Type.FALSE)) {
            throw new ParseException("unexpected non-literal token", first);
        }
        return first;
    }

    /**
     * Take a complete line from the stream, preserving whitespace, and EXCLUDING the newline at the end, which is
     * discarded.
     */
    public List<Token> takeLine() {
        var line = new ArrayList<Token>();
        while (!tokens.isEmpty()) {
            var next = tokens.remove(0);
            if (next.is(Token.Type.NEWLINE)) {
                return line;
            }
            line.add(next);
        }
        return line;
    }

    /**
     * Consumes a token of the expected type if it's at the head of the stream, doing nothing otherwise.
     */
    public void takeOptional(Token.Type type) {
        if (peek().is(type)) {
            take();
        }
    }

    /**
     * Traverse an expression enclosed by the given start and end tokens at the head of the stream.
     * The traversal will verify that the expression in the stream is balanced. The caller can and most likely will
     * still maintain their own operator stack, but it won't require balance checks.
     * Callers MAY consume any number of tokens from the front of the stream, as long as they do not remove enclosing
     * tokens in a manner that would disrupt the balance check.
     * The consumer will be invoked with both the starting and ending enclosing tokens.
     */
    public void walkExpression(Token.Type start, Token.Type end, Consumer<Token> consumer) {
        var state = new Stack<Token>();
        if (!peek().is(start)) {
            throw new ParseException("unexpected start of expression", peek());
        }

        while (isNotEmpty()) {
            var next = take();
            if (next.is(start)) {
                state.push(next);
            } else if (next.is(end)) {
                if (!state.isEmpty()) {
                    state.pop();
                } else {
                    throw new ParseException("unbalanced expression");
                }
            }

            consumer.accept(next);
            if (state.isEmpty()) { // balanced expression
                return;
            }
        }
        if (!state.isEmpty()) {
            throw new ParseException("unbalanced expression");
        }
    }

    /**
     * Take an entire expression denoted by start and end tokens from the head of the stream, optionally including those
     * enclosing tokens.
     */
    public TokenStream take(Token.Type start, Token.Type end, boolean preserveEnclosing) {
        var inner = new ArrayList<Token>();
        walkExpression(start, end, inner::add);
        if (!preserveEnclosing) {
            inner.remove(0);
            inner.remove(inner.size() - 1);
        }
        return new TokenStream(inner);
    }

    /**
     * Take an entire expression denoted by start and end tokens from the head of the stream, IGNORING those enclosing
     * tokens.
     */
    public TokenStream take(Token.Type start, Token.Type end) {
        return take(start, end, false);
    }

    /**
     * Consumes an argument list at the head of the stream matching the grammar ( expr0, expr1, <...> exprN [,] ).
     */
    public List<TokenStream> takeArgList() {
        var args = new ArrayList<TokenStream>();
        var current = new ArrayList<Token>();
        var expr = take(Token.Type.EXPR_START, Token.Type.EXPR_END);
        while (expr.isNotEmpty()) {
            var head = expr.peek();
            if (head.is(Token.Type.COMMA)) {
                if (current.isEmpty()) {
                    throw new ParseException("empty argument");
                } else {
                    args.add(new TokenStream(current));
                    current = new ArrayList<>();
                    expr.take();
                }
            } else if (head.is(Token.Type.EXPR_START)) {
                var nestedExpr = expr.take(Token.Type.EXPR_START, Token.Type.EXPR_END, true);
                current.addAll(nestedExpr.getTokens());
            } else if (head.is(Token.Type.LIST_START)) {
                var nestedExpr = expr.take(Token.Type.LIST_START, Token.Type.LIST_END, true);
                current.addAll(nestedExpr.getTokens());
            } else {
                current.add(expr.take());
            }
        }

        if (!current.isEmpty()) {
            args.add(new TokenStream(current));
        }
        return args;
    }

    /**
     * Attempt to parse the entire token stream as a list of strings.
     */
    public List<String> expectStringList() {
        return expectList().stream()
                .map(Token::expectString)
                .collect(Collectors.toList());
    }

    /**
     * Attempt to parse the entire token stream as a list of integers.
     */
    public List<Integer> expectIntList() {
        return expectList().stream()
                .map(Token::expectInt)
                .collect(Collectors.toList());
    }

    /**
     * Attempt to parse the entire token stream as a list of single, non-nested tokens.
     */
    public List<Token> expectList() {
        var tokens = new ArrayList<Token>();

        takeExpect(Token.Type.LIST_START);
        while (isNotEmpty()) {
            if (peek().is(Token.Type.LIST_END)) {
                return tokens;
            }

            tokens.add(take());
            if (peek().is(Token.Type.COMMA)) {
                take();
            } else if (peek().is(Token.Type.LIST_END)) {
                take();
                break;
            } else {
                throw new ParseException("unexpected token in list", peek());
            }
        }
        if (isNotEmpty()) {
            throw new ParseException("unterminated list");
        }

        return tokens;
    }

    public boolean isNotEmpty() { // this doesn't _currently_ need a version that checks non-semantic
        return tokens.stream().anyMatch(Token::isSemantic);
    }
}
