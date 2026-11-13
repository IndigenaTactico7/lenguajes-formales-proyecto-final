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

class LL1ParserTest {

    private static LL1Parser parser(String grammarText) {
        try {
            Grammar g = GrammarReader.read(new BufferedReader(new StringReader(grammarText)));
            return new LL1Parser(g, new FirstFollow(g));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void grammarWithNullableB() {
        LL1Parser parser = parser("3\nS -> AB\nA -> aA d\nB -> bBc e\n");
        assertTrue(parser.isLL1());
        assertTrue(parser.accepts("d"));
        assertTrue(parser.accepts("adbc"));
        assertTrue(parser.accepts("aadbbcc"));
        assertFalse(parser.accepts("a"));
        assertFalse(parser.accepts("e"));
        assertFalse(parser.accepts("adbcc"));
    }

    @Test
    void parseTreeForMinimalString() {
        LL1Parser parser = parser("3\nS -> AB\nA -> aA d\nB -> bBc e\n");
        Optional<ParseNode> tree = parser.parse("d");
        assertTrue(tree.isPresent());
        assertEquals("S[A[d], B[e]]", tree.get().toString());
    }

    @Test
    void leftRecursiveGrammarIsNotLL1() {
        LL1Parser parser = parser("3\nS -> S+T T\nT -> T*F F\nF -> (S) i\n");
        assertFalse(parser.isLL1());
    }

    @Test
    void ambiguousGrammarIsNotLL1() {
        LL1Parser parser = parser("1\nS -> SS a\n");
        assertFalse(parser.isLL1());
    }

    @Test
    void arithmeticExpressionGrammar() {
        LL1Parser parser = parser("5\nS -> TA\nA -> +TA e\nT -> FB\nB -> *FB e\nF -> (S) i\n");
        assertTrue(parser.isLL1());
        assertTrue(parser.accepts("i+i*i"));
        assertTrue(parser.accepts("(i)"));
        assertTrue(parser.accepts("i"));
        assertFalse(parser.accepts("i+"));
        assertFalse(parser.accepts(")i("));
        assertFalse(parser.accepts("ii"));
    }

    @Test
    void nestedPairGrammar() {
        LL1Parser parser = parser("1\nS -> aSb e\n");
        assertTrue(parser.isLL1());
        assertTrue(parser.accepts("e"));
        assertTrue(parser.accepts("ab"));
        assertTrue(parser.accepts("aabb"));
        assertFalse(parser.accepts("aab"));
        assertFalse(parser.accepts("ba"));
    }

    @Test
    void firstFollowConflictGrammarIsNotLL1() {
        LL1Parser parser = parser("2\nS -> Ab\nA -> b e\n");
        assertFalse(parser.isLL1());
    }

    @Test
    void parseRejectedInputReturnsEmpty() {
        LL1Parser parser = parser("3\nS -> AB\nA -> aA d\nB -> bBc e\n");
        assertTrue(parser.parse("a").isEmpty());
    }
}
