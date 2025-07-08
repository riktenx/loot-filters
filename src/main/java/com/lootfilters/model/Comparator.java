package com.lootfilters.model;

import com.lootfilters.lang.ParseException;
import com.lootfilters.lang.Token;

public enum Comparator {
    GT, LT, EQ, GT_EQ, LT_EQ;

    public static Comparator fromToken(Token t) {
        switch (t.getType()) {
            case OP_GT: return GT;
            case OP_LT: return LT;
            case OP_EQ: return EQ;
            case OP_GTEQ: return GT_EQ;
            case OP_LTEQ: return LT_EQ;
            default:
                throw new ParseException("unrecognized token", t);
        }
    }

    public static Comparator fromString(String str) {
        switch (str) {
            case ">": return GT;
            case "<": return LT;
            case "==": return EQ;
            case ">=": return GT_EQ;
            case "<=": return LT_EQ;
            default:
                throw new IllegalArgumentException("unrecognized comparator string " + str);
        }
    }
}
