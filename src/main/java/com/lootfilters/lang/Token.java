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
        IF, APPLY, RULE,
        META,
        COLON, COMMA,
        TRUE, FALSE, NIL,
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

    public static Token intLiteral(String value, Location location) { return new Token(Type.LITERAL_INT, value, location); }
    public static Token stringLiteral(String value, Location location) { return new Token(Type.LITERAL_STRING, value, location); }
    public static Token identifier(String value, Location location) { return new Token(Type.IDENTIFIER, value, location); }

    Type type;
    String value;
    Location location;

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

    public Color expectColor(boolean allowNil) {
        if (allowNil && type == Type.NIL) {
            return null;
        }

        if (type != Type.LITERAL_STRING) {
            throw new ParseException("unexpected non-string token", this);
        }

        var color = ColorUtil.fromHex(value);
        if (color == null) {
            throw new ParseException("unexpected non-color string", this);
        }
        return color;
    }

    public Color expectColor() {
        return expectColor(false);
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

    public Token withLocation(Location location) {
        return new Token(type, value, location);
    }

    @Override
    public String toString() {
        var str = "Token{type=" + type;
        return value != null && value.isEmpty()
                ? str + "}"
                : str + ",value=" + value + ", location=" + location.toString() + "}";
    }

    public String formatForException() {
        return String.format("%s at %s", value, location.toString());
    }
}
