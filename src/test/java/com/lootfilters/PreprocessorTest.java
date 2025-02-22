package com.lootfilters;

import com.lootfilters.lang.Lexer;
import com.lootfilters.lang.Preprocessor;
import com.lootfilters.lang.Token;
import org.junit.Test;

import java.util.stream.Collectors;

import static com.lootfilters.TestUtil.loadTestResource;
import static com.lootfilters.util.TextUtil.quote;
import static org.junit.Assert.assertEquals;

public class PreprocessorTest {
    @Test
    public void testPreprocess() throws Exception {
        var input = loadTestResource("preprocessor-test-input.rs2f");
        var expect = loadTestResource("preprocessor-test-expect.rs2f");

        var preprocessor = new Preprocessor(new Lexer("input", input).tokenize());
        var actual = preprocessor.preprocess().getTokens().stream()
                .map(it -> it.is(Token.Type.LITERAL_STRING) ? quote(it.getValue()) : it.getValue())
                .collect(Collectors.joining(""));
        assertEquals(expect, actual);
    }
}
