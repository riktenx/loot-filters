package com.lootfilters.lang;

public abstract class CompileException extends RuntimeException {
    protected CompileException(String message) {
        super(message);
    }
}
