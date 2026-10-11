package com.lootfilters.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class VarTransform {
    public static final VarTransform IDENTITY = new VarTransform(Collections.emptyList());

    @Getter
    private final List<Op> ops;

    public int apply(int value) {
        for (var op : ops) {
            value = op.apply(value);
        }
        return value;
    }

    public enum OpType {
        RSHIFT, LSHIFT, BITAND, BITOR
    }

    @AllArgsConstructor
    @Getter
    public static class Op {
        private final OpType type;
        private final int operand;

        public int apply(int value) {
            switch (type) {
                case RSHIFT: return value >> operand;
                case LSHIFT: return value << operand;
                case BITAND: return value & operand;
                case BITOR:  return value | operand;
                default: return value;
            }
        }
    }
}
