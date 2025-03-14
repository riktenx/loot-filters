package com.lootfilters;

import com.google.gson.Gson;
import com.lootfilters.lang.CompileException;
import com.lootfilters.lang.Lexer;
import com.lootfilters.lang.Parser;
import com.lootfilters.lang.Preprocessor;
import com.lootfilters.lang.Sources;
import com.lootfilters.lang.TokenStream;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.rule.Rule;
import com.lootfilters.serde.ColorDeserializer;
import com.lootfilters.serde.ColorSerializer;
import com.lootfilters.serde.RuleDeserializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.runelite.api.coords.WorldPoint;

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
    private final List<MatcherConfig> matchers;

    @Setter
    private String filename;

    public LootFilter(String name, String description, int[] activationArea, List<MatcherConfig> matchers) {
        this.name = name;
        this.description = description;
        this.activationArea = activationArea;
        this.matchers = matchers;
    }

    public static LootFilter fromJson(Gson gson, String json) {
        var ggson = gson.newBuilder()
                .registerTypeAdapter(Color.class, new ColorDeserializer())
                .registerTypeAdapter(Rule.class, new RuleDeserializer(gson))
                .create();
        return ggson.fromJson(json, LootFilter.class);
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

    public String toJson(Gson gson) {
        var ggson = gson.newBuilder()
                .registerTypeAdapter(Color.class, new ColorSerializer())
                .create();
        return ggson.toJson(this);
    }

    public DisplayConfig findMatch(LootFiltersPlugin plugin, PluginTileItem item) {
        DisplayConfig display = null;
        for (var matcher : matchers) {
            if (!matcher.getRule().test(plugin, item)) {
                continue;
            }

            if (matcher.isTerminal()) {
                return display == null
                        ? matcher.getDisplay()
                        : display.merge(matcher.getDisplay());
            } else {
                display = display == null
                        ? matcher.getDisplay()
                        : display.merge(matcher.getDisplay());
            }
        }
        return display;
    }

    public boolean isInActivationArea(WorldPoint p) {
        if (activationArea == null) {
            return false;
        }
        return p.getX() >= activationArea[0] && p.getY() >= activationArea[1] && p.getPlane() >= activationArea[2]
                && p.getX() <= activationArea[3] && p.getY() <= activationArea[4] && p.getPlane() <= activationArea[5];
    }
}
