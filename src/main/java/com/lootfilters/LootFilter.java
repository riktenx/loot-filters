package com.lootfilters;

import com.lootfilters.lang.CompileException;
import com.lootfilters.lang.Lexer;
import com.lootfilters.lang.Parser;
import com.lootfilters.lang.Preprocessor;
import com.lootfilters.lang.Sources;
import com.lootfilters.lang.TokenStream;
import com.lootfilters.lang.TokenizeException;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lootfilters.util.TextUtil.normalizeCrlf;

@Getter
@EqualsAndHashCode
@ToString
public class LootFilter {
    public static final LootFilter Nop = LootFilter.builder().build();

    private final String name;
    private final String filename;
    private final String description;
    private final List<FilterRule> rules;

    private LootFilter(Builder builder) {
        name = builder.name;
        filename = builder.filename;
        description = builder.description;
        rules = builder.rules;
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
                .map(it -> {
					try {
						return it.tokenize();
					} catch (TokenizeException e) {
						throw new RuntimeException(e);
					}
				})
                .flatMap(tokenStream -> tokenStream.getTokens().stream())
                .collect(Collectors.collectingAndThen(Collectors.toList(), TokenStream::new));

        var postproc = new Preprocessor(combinedStream).preprocess();
        return new Parser(postproc).parse();
    }

	public static LootFilter fromSource(String filename, String source) throws CompileException {
		var filter = fromSourcesWithPreamble(Map.of(filename, source))
			.toBuilder()
			.setFilename(filename);
		if (filter.name == null || filter.name.isBlank()) {
			filter.setName(filename);
		}
		return filter.build();
	}

    public Builder toBuilder() {
        var builder = new Builder();
        builder.name = name;
        builder.filename = filename;
        builder.description = description;
        builder.rules = new ArrayList<>(rules);
        return builder;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String filename;
        private String description;
        private List<FilterRule> rules = new ArrayList<>();

        private Builder() {
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setRules(List<FilterRule> rules) {
            this.rules = rules;
            return this;
        }

        public Builder addRule(FilterRule rule) {
            rules.add(rule);
            return this;
        }

        public LootFilter build() {
            return new LootFilter(this);
        }
    }
}
