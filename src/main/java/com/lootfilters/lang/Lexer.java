package com.lootfilters.lang;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.lootfilters.util.TextUtil.isLegalIdent;
import static com.lootfilters.util.TextUtil.isNumeric;
import static com.lootfilters.util.TextUtil.isWhitespace;

@RequiredArgsConstructor
public class Lexer {
    private static final LinkedHashMap<String, Token.Type> STATICS = new LinkedHashMap<>() {{
        put("\\\n", Token.Type.WHITESPACE);
        put("#define", Token.Type.PREPROC_DEFINE);
        put("apply", Token.Type.APPLY);
        put("false", Token.Type.FALSE);
        put("true", Token.Type.TRUE);
        put("meta", Token.Type.META);
        put("if", Token.Type.IF);
        put("&&", Token.Type.OP_AND);
        put("||", Token.Type.OP_OR);
        put(">=", Token.Type.OP_GTEQ);
        put("<=", Token.Type.OP_LTEQ);
        put("==", Token.Type.OP_EQ);
        put("!", Token.Type.OP_NOT);
        put(">", Token.Type.OP_GT);
        put("<", Token.Type.OP_LT);
        put(";", Token.Type.STMT_END);
        put(":", Token.Type.COLON);
        put("=", Token.Type.ASSIGN);
        put(",", Token.Type.COMMA);
        put("(", Token.Type.EXPR_START);
        put(")", Token.Type.EXPR_END);
        put("{", Token.Type.BLOCK_START);
        put("}", Token.Type.BLOCK_END);
        put("[", Token.Type.LIST_START);
        put("]", Token.Type.LIST_END);
        put("\n", Token.Type.NEWLINE);
        put("\r", Token.Type.NEWLINE);
    }};

    private final String input;
    private final List<Token> tokens = new ArrayList<>();
    private int offset = 0;

    public List<Token> tokenize() throws TokenizeException {
        while (offset < input.length()) {
            if (tokenizeStatic()) {
                continue;
            }
            if (tokenizeComment()) {
                continue;
            }

            var ch = input.charAt(offset);
            if (isWhitespace(ch)) {
                tokenizeWhitespace();
            } else if (isNumeric(ch)) {
                tokenizeLiteralInt();
            } else if (ch == '"') {
                tokenizeLiteralString();
            } else if (isLegalIdent(ch)) {
                tokenizeIdentifier();
            } else {
                throw new TokenizeException("unrecognized character '" + ch + "'");
            }
        }

        return tokens.stream() // un-map escaped newlines
                .map(it -> it.getValue().equals("\\\n") ? new Token(Token.Type.WHITESPACE, "") : it)
                .collect(Collectors.toList());
    }

    private boolean tokenizeStatic() {
        for (var entry : STATICS.entrySet()) {
            var value = entry.getKey();
            var type = entry.getValue();
            if (input.startsWith(value, offset)) {
                tokens.add(new Token(type, value));
                offset += value.length();
                return true;
            }
        }
        return false;
    }

    private boolean tokenizeComment() {
        if (!input.startsWith("//", offset)) {
            return false;
        }

        var lineEnd = input.indexOf('\n', offset);
        var text = lineEnd > -1
                ? input.substring(offset, lineEnd)
                : input.substring(offset);
        tokens.add(new Token(Token.Type.COMMENT, text));
        offset += text.length();
        return true;
    }

    private void tokenizeWhitespace() {
        for (int i = offset; i < input.length(); ++i) {
            if (!isWhitespace(input.charAt(i))) {
                var ws = input.substring(offset, i);
                tokens.add(new Token(Token.Type.WHITESPACE, ws));
                offset += i - offset;
                return;
            }
        }
        tokens.add(new Token(Token.Type.WHITESPACE, input.substring(offset)));
        offset = input.length();
    }

    private void tokenizeLiteralInt() {
        for (int i = offset; i < input.length(); ++i) {
            if (input.charAt(i) == '_') {
                continue;
            }
            if (!isNumeric(input.charAt(i))) {
                var literal = input.substring(offset, i);
                tokens.add(Token.intLiteral(literal));
                offset += literal.length();
                return;
            }
        }
        tokens.add(Token.intLiteral(input.substring(offset)));
        offset = input.length();
    }

    private void tokenizeLiteralString() throws TokenizeException {
        for (int i = offset+1; i < input.length(); ++i) {
            if (input.charAt(i) == '"') {
                var literal = input.substring(offset+1, i);
                tokens.add(Token.stringLiteral(literal));
                offset += literal.length() + 2; // for quotes, which the captured literal omits
                return;
            }
        }
        throw new TokenizeException("unterminated string literal");
    }

    private void tokenizeIdentifier() {
        for (int i = offset; i < input.length(); ++i) {
            if (!isLegalIdent(input.charAt(i))) {
                var ident = input.substring(offset, i);
                tokens.add(Token.identifier(ident));
                offset += ident.length();
                return;
            }
        }
        tokens.add(Token.identifier(input.substring(offset)));
        offset = input.length();
    }
}
