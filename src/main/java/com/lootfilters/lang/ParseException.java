package com.lootfilters.lang;

import lombok.Getter;

import static com.lootfilters.lang.Location.UNKNOWN_SOURCE_NAME;

@Getter
public class ParseException extends CompileException {
    private final Token token;

    public ParseException(String message, Token token) {
        super(message + " " + formatToken(token));
        this.token = token;
    }

    public ParseException(String message) {
        super(message);
        this.token = null;
    }

    private static String formatToken(Token token) {
        // Format a message with source names only if available
        if (token.getLocation().getMacroSourceLocation() != null) {
            var tokenLocation = token.getLocation();
            var macroLocation = tokenLocation.getMacroSourceLocation();
            var expansionErrorAndLocation = String.format("'%s' after expanding macro %s on L%d, C%d%s.",
                    token.getValue(),
                    macroLocation.getMacroName(),
                    tokenLocation.getLineNumber(),
                    tokenLocation.getCharNumber(),
                    sourceNameFragment(tokenLocation.getSourceName()));

            var macroDefineAndMacroErrorLocation = "";
            if (!macroLocation.getSourceName().equals(UNKNOWN_SOURCE_NAME)) {
                macroDefineAndMacroErrorLocation = String.format(" %s is defined in '%s', error was on L%d, C%d.",
                        macroLocation.getMacroName(),
                        macroLocation.getSourceName(),
                        macroLocation.getLineNumber(),
                        macroLocation.getCharNumber()
                );
            }

            return expansionErrorAndLocation + macroDefineAndMacroErrorLocation;
        } else {
            return String.format("'%s' on L%s, C%s%s",
                    token.getValue(),
                    token.getLocation().getLineNumber(),
                    token.getLocation().getCharNumber(),
                    sourceNameFragment(token.getLocation().getSourceName())
            );
        }
    }

    private static String sourceNameFragment(String sourceName) {
        return sourceName.equals(UNKNOWN_SOURCE_NAME) ? "" : String.format(" in '%s'", sourceName);
    }
}
