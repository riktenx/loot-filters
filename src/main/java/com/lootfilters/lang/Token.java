package com.lootfilters.lang;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.client.util.ColorUtil;

import java.awt.Color;

@Value
@RequiredArgsConstructor
public class Token {
    public enum Type {
        WHITESPACE, NEWLINE,
        IF, APPLY,
        META,
        COLON, COMMA,
        TRUE, FALSE,
        IDENTIFIER,
        LITERAL_INT, LITERAL_STRING,
        ASSIGN,
        OP_EQ, OP_GT, OP_LT, OP_GTEQ, OP_LTEQ, OP_AND, OP_OR, OP_NOT,
        EXPR_START, EXPR_END,
        BLOCK_START, BLOCK_END,
        LIST_START, LIST_END,
        STMT_END,
        PREPROC_DEFINE,
        COMMENT,
    }

    public static Token intLiteral(String value) { return new Token(Type.LITERAL_INT, value); }
    public static Token stringLiteral(String value) { return new Token(Type.LITERAL_STRING, value); }
    public static Token identifier(String value) { return new Token(Type.IDENTIFIER, value); }

    Type type;
    String value;

    public boolean is(Type type) {
        return this.type == type;
    }

    public int expectInt() {
        if (type != Type.LITERAL_INT) {
            throw new ParseException("unexpected non-int token", this);
        }
        return Integer.parseInt(value.replace("_", ""));
    }

    public String expectString() {
        if (type != Type.LITERAL_STRING) {
            throw new ParseException("unexpected non-string token", this);
        }
        return value;
    }

    public Color expectColor() {
        if (type != Type.LITERAL_STRING) {
            throw new ParseException("unexpected non-string token", this);
        }

        var color = ColorUtil.fromHex(value);
        if (color == null) {
            throw new ParseException("unexpected non-color string", this);
        }
        return color;
    }

    public boolean expectBoolean() {
        switch (type) {
            case TRUE: return true;
            case FALSE: return false;
            default:
                throw new ParseException("unexpected non-boolean token", this);
        }
    }

    public boolean isWhitespace() {
        return type == Type.WHITESPACE || type == Type.NEWLINE;
    }

    public boolean isSemantic() {
        return type != Type.COMMENT && !isWhitespace();
    }

    @Override
    public String toString() {
        var str = "Token{type=" + type;
        return value != null && value.isEmpty()
                ? str + "}"
                : str + ",value=" + value + "}";
    }
}
