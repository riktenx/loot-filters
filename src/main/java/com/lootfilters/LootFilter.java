package com.lootfilters;

import com.lootfilters.lang.CompileException;
import com.lootfilters.lang.Lexer;
import com.lootfilters.lang.Parser;
import com.lootfilters.lang.Preprocessor;
import com.lootfilters.lang.Sources;
import com.lootfilters.lang.TokenStream;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lootfilters.lang.Location.UNKNOWN_SOURCE_NAME;
import static com.lootfilters.util.TextUtil.normalizeCrlf;

@Getter
@EqualsAndHashCode
@ToString
public class LootFilter {
    public static final LootFilter Nop = new LootFilter("", "", null, new ArrayList<>());

    @Setter
    private String name; // anonymous filter can be imported, would need name

    private final String description;
    private final int[] activationArea;
    private final List<FilterRule> rules;

    @Setter
    private String filename;

    public LootFilter(String name, String description, int[] activationArea, List<FilterRule> rules) {
        this.name = name;
        this.description = description;
        this.activationArea = activationArea;
        this.rules = rules;
    }

    public static LootFilter fromSourcesWithPreamble(Map<String, String> sources) throws CompileException {
        if (!sources.containsKey("preamble")) {
            // LinkedHashMap preserves insertion order for iteration ensuring preamble is handled first
            var withPreamble = new LinkedHashMap<String, String>();
            withPreamble.put("preamble", Sources.getPreamble());
            // If iteration order of input map is unstable this may change ordering of input scripts
            withPreamble.putAll(sources);
            return fromSources(withPreamble);
        }
        return fromSources(new LinkedHashMap<>(sources));
    }

    public static LootFilter fromSources(LinkedHashMap<String, String> sources) throws CompileException {
        var combinedStream = sources
                .entrySet().stream()
                .map(source -> {
                    // Do this in 1 map iteration to ensure we preserve iteration order over our input
                    var sourceValue = source.getValue();
                    if (!sourceValue.endsWith("\n")) {
                        sourceValue += "\n";
                    }
                    return new Lexer(source.getKey(), normalizeCrlf(sourceValue));
                })
                .map(Lexer::tokenize)
                .flatMap(tokenStream -> tokenStream.getTokens().stream())
                .collect(Collectors.collectingAndThen(Collectors.toList(), TokenStream::new));

        var postproc = new Preprocessor(combinedStream).preprocess();
        return new Parser(postproc).parse();
    }

    public static LootFilter fromSource(String source) throws CompileException {
        return fromSourcesWithPreamble(Map.of(UNKNOWN_SOURCE_NAME, source));
    }

    public @NonNull DisplayConfig findMatch(LootFiltersPlugin plugin, PluginTileItem item) {
        var display = new DisplayConfig(Color.WHITE).toBuilder()
                .compact(plugin.getConfig().compactMode())
                .build();
        for (var rule : rules) {
            if (!rule.getCond().test(plugin, item)) {
                continue;
            }

            display = display.merge(rule.getDisplay());
            display.getEvalTrace().add(rule.getSourceLine());
            if (rule.isTerminal()) {
                return display;
            }
        }
        return display;
    }
}
