package com.lootfilters.lang;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.lootfilters.util.TextUtil.isLegalIdent;
import static com.lootfilters.util.TextUtil.isNumeric;

@RequiredArgsConstructor
public class Lexer {
    private static final LinkedHashMap<String, Token.Type> STATICS = new LinkedHashMap<>() {{
        put("\\\n", Token.Type.WHITESPACE);
        put("#define", Token.Type.PREPROC_DEFINE);
        put("apply", Token.Type.APPLY);
        put("false", Token.Type.FALSE);
        put("true", Token.Type.TRUE);
        put("meta", Token.Type.META);
        put("rule", Token.Type.RULE);
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

    private final String inputName;
    private final String input;
    private final List<Token> tokens = new ArrayList<>();
    private int offset = 0;
    // 1 indexed cause editors start line and char counts at 1
    private int currentLineOffset = 1;
    private int currentLineNumber = 1;

    public TokenStream tokenize() throws TokenizeException {
        while (offset < input.length()) {
            if (tokenizeStatic()) {
                continue;
            }
            if (tokenizeComment()) {
                continue;
            }

            var ch = input.charAt(offset);
            if (isTokenWhitespace(ch)) {
                tokenizeWhitespace();
            } else if (isNumeric(ch) || ch == '-' && isNumeric(input.charAt(offset + 1))) {
                tokenizeLiteralInt();
            } else if (ch == '"') {
                tokenizeLiteralString();
            } else if (isLegalIdent(ch)) {
                tokenizeIdentifier();
            } else {
                throw new TokenizeException(String.format("unrecognized character '" + ch + "' line %s char %s", currentLineNumber, currentLineOffset));
            }
        }

        return tokens.stream() // un-map escaped newlines
                .map(it -> it.getValue().equals("\\\n") ? new Token(Token.Type.WHITESPACE, "", it.getLocation()) : it)
                .collect(Collectors.collectingAndThen(Collectors.toList(), TokenStream::new));
    }

    private boolean tokenizeStatic() {
        for (var entry : STATICS.entrySet()) {
            var value = entry.getKey();
            var type = entry.getValue();
            if (input.startsWith(value, offset)) {
                tokens.add(new Token(type, value, currentLocation()));
                currentLineOffset += value.length();
                offset += value.length();
                // Checking for \n here captures both escaped and unescaped newlines
                // allowing us to properly track position within newline-escaped macros
                if (value.endsWith("\n")) {
                    currentLineOffset = 1;
                    currentLineNumber += 1;
                }
                return true;
            }
        }
        return false;
    }

    private boolean tokenizeComment() {
        return tokenizeLineComment() || tokenizeBlockComment();
    }

    private boolean tokenizeLineComment() {
        if (!input.startsWith("//", offset)) {
            return false;
        }

        var lineEnd = input.indexOf('\n', offset);
        var text = lineEnd > -1
                ? input.substring(offset, lineEnd)
                : input.substring(offset);
        tokens.add(new Token(Token.Type.COMMENT, text, currentLocation()));
        currentLineOffset += text.length();
        offset += text.length();
        return true;
    }

    private boolean tokenizeBlockComment() {
        if (!input.startsWith("/*", offset)) {
            return false;
        }

        for (var i = offset + 2; i < input.length(); ++i) {
            if (input.startsWith("*/", i)) {
                currentLineOffset += 2;
                var text = input.substring(offset, i + 2);
                tokens.add(new Token(Token.Type.COMMENT, text, currentLocation()));
                offset += text.length();
                return true;
            } else if (input.charAt(i) == '\n') {
                ++currentLineNumber;
                currentLineOffset = 0;
            } else {
                ++currentLineOffset;
            }
        }

        throw new TokenizeException(String.format("unterminated block comment at line %d, char %d", currentLineNumber, currentLineOffset));
    }

    private void tokenizeWhitespace() {
        for (int i = offset; i < input.length(); ++i) {
            if (!isTokenWhitespace(input.charAt(i))) {
                var ws = input.substring(offset, i);
                tokens.add(new Token(Token.Type.WHITESPACE, ws, currentLocation()));
                currentLineOffset += i - offset;
                offset += i - offset;
                return;
            }
        }
        var ws = input.substring(offset);
        tokens.add(new Token(Token.Type.WHITESPACE, ws, currentLocation()));
        currentLineOffset += ws.length();
        offset = input.length();
    }

    private void tokenizeLiteralInt() {
        var start = input.charAt(offset) == '-' ? offset + 1 : offset;
        for (int i = start; i < input.length(); ++i) {
            if (input.charAt(i) == '_') {
                continue;
            }
            if (!isNumeric(input.charAt(i))) {
                var literal = input.substring(offset, i);
                tokens.add(Token.intLiteral(literal, currentLocation()));
                currentLineOffset += literal.length();
                offset += literal.length();
                return;
            }
        }
        tokens.add(Token.intLiteral(input.substring(offset), currentLocation()));
        currentLineOffset += input.substring(offset).length();
        offset = input.length();
    }

    private void tokenizeLiteralString() throws TokenizeException {
        for (int i = offset + 1; i < input.length(); ++i) {
            if (input.charAt(i) == '"') {
                var literal = input.substring(offset + 1, i);
                // for quotes, which the captured literal omits
                tokens.add(Token.stringLiteral(literal, currentLocation()));
                currentLineOffset += literal.length() + 2;
                offset += literal.length() + 2;
                return;
            }
        }
        throw new TokenizeException("unterminated string literal");
    }

    private void tokenizeIdentifier() {
        for (int i = offset; i < input.length(); ++i) {
            if (!isLegalIdent(input.charAt(i))) {
                var ident = input.substring(offset, i);
                tokens.add(Token.identifier(ident, currentLocation()));
                currentLineOffset += ident.length();
                offset += ident.length();
                return;
            }
        }
        tokens.add(Token.identifier(input.substring(offset), currentLocation()));
        currentLineOffset += input.substring(offset).length();
        offset = input.length();
    }

    private Location currentLocation() {
        return new Location(inputName, currentLineNumber, currentLineOffset);
    }

    private static boolean isTokenWhitespace(char c) { // newlines are tokenized separately
        return c == ' ' || c == '\t';
    }
}
