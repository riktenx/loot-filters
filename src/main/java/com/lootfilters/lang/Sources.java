package com.lootfilters.lang;

import com.lootfilters.LootFilter;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;

import static com.lootfilters.util.TextUtil.normalizeCrlf;

public class Sources {
    @Getter private static String preamble;
    @Getter private static String referenceSource;

    @Getter private static LootFilter referenceFilter;

    private Sources() {}

    static {
        try (
            var preambleStream = Sources.class.getResourceAsStream("/com/lootfilters/scripts/preamble.rs2f");
            var referenceSourceStream = Sources.class.getResourceAsStream("/com/lootfilters/scripts/filterscape.rs2f");
        ) {
            preamble = loadScriptResource(preambleStream);
            referenceSource = loadScriptResource(referenceSourceStream);

            referenceFilter = LootFilter.fromSource(referenceSource);
        } catch (IOException e) {
            throw new RuntimeException("init static sources", e);
        } catch (CompileException e) {
            throw new RuntimeException("init static filters", e);
        }
    }

    private static String loadScriptResource(InputStream in) throws IOException {
        return normalizeCrlf(new String(in.readAllBytes()));
    }
}
