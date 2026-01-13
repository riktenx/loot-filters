package com.lootfilters.lang;

import java.util.zip.GZIPInputStream;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;

import static com.lootfilters.util.TextUtil.normalizeCrlf;

public class Sources {
    @Getter private static String preamble;

    private Sources() {}

    static {
        try (
            var preambleStream = Sources.class.getResourceAsStream("/com/lootfilters/scripts/preamble.rs2f");
        ) {
            preamble = loadScriptResource(preambleStream);
        } catch (IOException e) {
            throw new RuntimeException("init static sources", e);
        } catch (CompileException e) {
            throw new RuntimeException("init static filters", e);
        }
    }

    public static String loadScriptResource(InputStream in) throws IOException {
        return normalizeCrlf(new String(in.readAllBytes()));
    }

	public static String loadScriptResource(Class<?> clazz, String name, boolean gzip) throws IOException {
		try (
			var inner = clazz.getResourceAsStream(name);
			var stream = gzip ? new GZIPInputStream(inner) : inner;
		) {
			return loadScriptResource(stream);
		}
	}
}