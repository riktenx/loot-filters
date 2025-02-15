package com.lootfilters;

import com.google.gson.Gson;
import com.lootfilters.lang.CompileException;
import com.lootfilters.lang.Lexer;
import com.lootfilters.lang.Parser;
import com.lootfilters.lang.Preprocessor;
import com.lootfilters.lang.Sources;
import com.lootfilters.rule.Rule;
import com.lootfilters.serde.ColorDeserializer;
import com.lootfilters.serde.ColorSerializer;
import com.lootfilters.serde.RuleDeserializer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

import java.awt.Color;
import java.util.List;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class LootFilter {
    @Setter private String name; // anonymous filter can be imported, would need name

    private final String description;
    private final int[] activationArea;
    private final List<MatcherConfig> matchers;

    public static LootFilter fromJson(Gson gson, String json) {
        var ggson = gson.newBuilder()
                .registerTypeAdapter(Color.class, new ColorDeserializer())
                .registerTypeAdapter(Rule.class, new RuleDeserializer(gson))
                .create();
        return ggson.fromJson(json, LootFilter.class);
    }

    public static LootFilter fromSource(String source) throws CompileException {
        var postproc = new Preprocessor(Sources.getPreamble() + "\n" + source).preprocess();
        var tokens = new Lexer(postproc).tokenize();
        return new Parser(tokens).parse();
    }

    public String toJson(Gson gson) {
        var ggson = gson.newBuilder()
                .registerTypeAdapter(Color.class, new ColorSerializer())
                .create();
        return ggson.toJson(this);
    }

    public DisplayConfig findMatch(LootFiltersPlugin plugin, TileItem item) {
        var match = matchers.stream()
                .filter(it -> it.getRule().test(plugin, item))
                .findFirst()
                .orElse(null);
        return match != null ? match.getDisplay() : null;
    }

    public boolean isInActivationArea(WorldPoint p) {
        if (activationArea == null) {
            return false;
        }
        return p.getX() >= activationArea[0] && p.getY() >= activationArea[1] && p.getPlane() >= activationArea[2]
                && p.getX() <= activationArea[3] && p.getY() <= activationArea[4] && p.getPlane() <= activationArea[5];
    }
}
