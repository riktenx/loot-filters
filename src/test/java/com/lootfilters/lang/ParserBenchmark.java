package com.lootfilters.lang;

import com.lootfilters.DefaultFilter;
import com.lootfilters.LootFilter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.lootfilters.util.TextUtil.normalizeCrlf;

public class ParserBenchmark {
    private static final int WARMUP = 20;
    private static final int ITERATIONS = 100;

    public static void main(String[] args) throws Exception {
        var sources = loadSources();

        System.out.println("src:");
        for (var name : sources.keySet()) {
            System.out.printf("  %s (%,d chars)\n", name, sources.get(name).length());
        }
        System.out.println();

        for (int i = 0; i < WARMUP; i++) {
            parse(preprocess(tokenize(sources)));
        }

        long[] tokenizeTimes = new long[ITERATIONS];
        long[] preprocessTimes = new long[ITERATIONS];
        long[] parseTimes = new long[ITERATIONS];

        for (int i = 0; i < ITERATIONS; i++) {
            long t0 = System.nanoTime();
            var tokenized = tokenize(sources);
            long t1 = System.nanoTime();
            var preprocessed = preprocess(tokenized);
            long t2 = System.nanoTime();
            parse(preprocessed);
            long t3 = System.nanoTime();

            tokenizeTimes[i] = t1 - t0;
            preprocessTimes[i] = t2 - t1;
            parseTimes[i] = t3 - t2;
        }

        long[] totalTimes = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            totalTimes[i] = tokenizeTimes[i] + preprocessTimes[i] + parseTimes[i];
        }

        System.out.printf("%-12s  %8s  %8s  %8s\n", "stage", "mean", "min", "max");
        System.out.printf("%-12s  %8s  %8s  %8s\n", "------------", "--------", "--------", "--------");
        report("tokenize", tokenizeTimes);
        report("preprocess", preprocessTimes);
        report("parse", parseTimes);
        report("total", totalTimes);
    }

    private static Map<String, String> loadSources() throws Exception {
        var preamble = Sources.getPreamble();
        var riktens = Sources.loadScriptResource(DefaultFilter.class, "defaultriktensfilter.rs2f.gz", true);

        var sources = new LinkedHashMap<String, String>();
        sources.put("preamble", preamble);
        sources.put("defaultriktensfilter.rs2f.gz", riktens);
        return sources;
    }

    private static TokenStream tokenize(Map<String, String> sources) throws TokenizeException {
        var allTokens = new ArrayList<Token>();
        for (var entry : sources.entrySet()) {
            var src = normalizeCrlf(entry.getValue());
            if (!src.endsWith("\n")) {
                src += "\n";
            }
            allTokens.addAll(new Lexer(entry.getKey(), src).tokenize().getTokens());
        }
        return new TokenStream(allTokens);
    }

    private static TokenStream preprocess(TokenStream tokenStream) throws PreprocessException {
        return new Preprocessor(tokenStream).preprocess();
    }

    private static LootFilter parse(TokenStream preprocessed) throws ParseException {
        return new Parser(preprocessed).parse();
    }

    private static void report(String stage, long[] times) {
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (long t : times) {
            sum += t;
            if (t < min) min = t;
            if (t > max) max = t;
        }
        double meanMs = sum / (double) times.length / 1_000_000.0;
        double minMs = min / 1_000_000.0;
        double maxMs = max / 1_000_000.0;
        System.out.printf("%-12s  %7.2f ms  %7.2f ms  %7.2f ms%n", stage, meanMs, minMs, maxMs);
    }
}