package com.lootfilters.lang;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lootfilters.util.CollectionUtil.append;
import static com.lootfilters.util.TextUtil.normalizeCrlf;
import static com.lootfilters.util.TextUtil.quote;
import static java.util.Collections.emptyList;

public class Preprocessor {
    private final TokenStream tokens;

    private final Map<String, Define> defines = new HashMap<>();
    private final List<Token> preproc = new ArrayList<>(); // pre-expansion w/ all preproc lines removed

    public Preprocessor(String input) {
        input = normalizeCrlf(input);
        this.tokens = new TokenStream(new Lexer(input).tokenize());
    }

    public String preprocess() throws PreprocessException {
        while (tokens.isNotEmpty()) {
            var next = tokens.take(true);
            if (next.is(Token.Type.PREPROC_DEFINE)) {
                parseDefine();
            } else {
                preproc.add(next);
                if (!next.is(Token.Type.NEWLINE)) {
                    preproc.addAll(tokens.takeLine());
                    preproc.add(new Token(Token.Type.NEWLINE, "\n"));
                }
            }
        }

        return expandDefines(emptyList(), new TokenStream(preproc)).stream()
                .map(it -> it.is(Token.Type.LITERAL_STRING) ? quote(it.getValue()) : it.getValue())
                .collect(Collectors.joining(""))
                .trim();
    }

    private void parseDefine() {
        var name = tokens.takeExpect(Token.Type.IDENTIFIER).getValue();
        var params = tokens.peek().is(Token.Type.EXPR_START)
                ? parseDefineParams() : null;
        if (params != null && params.isEmpty()) {
            throw new PreprocessException("#define " + quote(name) + " has empty param list");
        }
        tokens.takeExpect(Token.Type.WHITESPACE, true);
        defines.put(name, new Define(name, params, tokens.takeLine()));
    }

    private List<String> parseDefineParams() {
        var params = new ArrayList<String>();
        tokens.takeExpect(Token.Type.EXPR_START);
        while (tokens.isNotEmpty()) {
            var next = tokens.take();
            if (next.is(Token.Type.EXPR_END)) {
                return params;
            } else if (next.is(Token.Type.IDENTIFIER)) {
                params.add(next.getValue());
                tokens.takeOptional(Token.Type.COMMA);
            } else {
                throw new PreprocessException("unterminated define param list");
            }
        }
        throw new PreprocessException("unterminated define param list");
    }

    private List<Token> expandDefines(List<String> visited, TokenStream tokens) {
        var postproc = new ArrayList<Token>();
        while (tokens.isNotEmpty()) {
            var token = tokens.take(true);
            if (!visited.contains(token.getValue()) && token.is(Token.Type.IDENTIFIER) && defines.containsKey(token.getValue())) {
                var define = defines.get(token.getValue());
                if (define.isParameterized()) {
                    var args = tokens.takeArgList();
                    postproc.addAll(expandParameterizedDefine(append(visited, define.name), define, args));
                } else {
                    var defineTokens = new TokenStream(new ArrayList<>(define.value));
                    postproc.addAll(expandDefines(append(visited, define.name), defineTokens));
                }
            } else {
                postproc.add(token);
            }
        }
        return postproc;
    }

    private List<Token> expandParameterizedDefine(List<String> visited, Define define, List<TokenStream> args) {
        var expanded = new ArrayList<Token>();
        for (var token : define.value) {
            if (!token.is(Token.Type.IDENTIFIER) || token.getValue().equals(define.name)) {
                expanded.add(token);
                continue;
            }

            var paramIndex = -1;
            for (var i = 0; i < define.params.size(); ++i) {
                if (define.params.get(i).equals(token.getValue())) {
                    paramIndex = i;
                    break;
                }
            }
            if (paramIndex > -1) {
                var arg = args.get(paramIndex);
                expanded.addAll(arg.getTokens());
            } else {
                expanded.add(token);
            }
        }
        return expandDefines(visited, new TokenStream(expanded));
    }

    @AllArgsConstructor
    private static class Define {
       final String name;
       final List<String> params;
       final List<Token> value;

       boolean isParameterized() {
           return params != null;
       }
    }
}
