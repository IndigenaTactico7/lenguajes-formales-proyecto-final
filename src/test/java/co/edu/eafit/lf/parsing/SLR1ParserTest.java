package co.edu.eafit.lf.parsing;

import co.edu.eafit.lf.analysis.FirstFollow;
import co.edu.eafit.lf.grammar.Grammar;
import co.edu.eafit.lf.grammar.GrammarReader;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SLR1ParserTest {

    private static SLR1Parser parser(String grammarText) {
        try {
            Grammar g = GrammarReader.read(new BufferedReader(new StringReader(grammarText)));
            return new SLR1Parser(g, new FirstFollow(g));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void arithmeticGrammar() {
        SLR1Parser parser = parser("3\nS -> S+T T\nT -> T*F F\nF -> (S) i\n");
        assertTrue(parser.isSLR1());
        assertTrue(parser.accepts("i+i"));
        assertTrue(parser.accepts("(i)"));
        assertTrue(parser.accepts("i*i+i"));
        assertFalse(parser.accepts("(i+i)*i)"));
        assertFalse(parser.accepts("i+"));
    }

    @Test
    void grammarWithNullableB() {
        SLR1Parser parser = parser("3\nS -> AB\nA -> aA d\nB -> bBc e\n");
        assertTrue(parser.isSLR1());
        assertTrue(parser.accepts("d"));
        assertTrue(parser.accepts("adbc"));
        assertFalse(parser.accepts("a"));
    }

    @Test
    void parseTreeForMinimalString() {
        SLR1Parser parser = parser("3\nS -> AB\nA -> aA d\nB -> bBc e\n");
        Optional<ParseNode> tree = parser.parse("d");
        assertTrue(tree.isPresent());
        assertEquals("S[A[d], B[e]]", tree.get().toString());
    }

    @Test
    void ambiguousGrammarIsNotSLR1() {
        SLR1Parser parser = parser("1\nS -> SS a\n");
        assertFalse(parser.isSLR1());
    }

    @Test
    void nestedPairGrammar() {
        SLR1Parser parser = parser("1\nS -> aSb e\n");
        assertTrue(parser.isSLR1());
        assertTrue(parser.accepts("e"));
        assertTrue(parser.accepts("aabb"));
        assertFalse(parser.accepts("aab"));
    }

    @Test
    void shiftReduceConflictGrammarIsNotSLR1() {
        SLR1Parser parser = parser("3\nS -> L=R R\nL -> *R i\nR -> L\n");
        assertFalse(parser.isSLR1());
    }

    @Test
    void reduceReduceConflictGrammarIsNotSLR1() {
        SLR1Parser parser = parser("3\nS -> AaAb BbBa\nA -> e\nB -> e\n");
        assertFalse(parser.isSLR1());
    }
}
