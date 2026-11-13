package co.edu.eafit.lf.analysis;

import co.edu.eafit.lf.grammar.Grammar;
import co.edu.eafit.lf.grammar.GrammarReader;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FirstFollowTest {

    private static List<String> sets(String grammarText) {
        try {
            Grammar g = GrammarReader.read(new BufferedReader(new StringReader(grammarText)));
            return new FirstFollow(g).formatSets().lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void expressionGrammarWithLeftRecursion() {
        assertEquals(
                List.of(
                        "First(F) = {(, i}",
                        "First(S) = {(, i}",
                        "First(T) = {(, i}",
                        "Follow(F) = {), *, +, $}",
                        "Follow(S) = {), +, $}",
                        "Follow(T) = {), *, +, $}"
                ),
                sets("3\nS -> S+T T\nT -> T*F F\nF -> (S) i\n")
        );
    }

    @Test
    void epsilonInNonTerminalB() {
        assertEquals(
                List.of(
                        "First(A) = {a, d}",
                        "First(B) = {b, e}",
                        "First(S) = {a, d}",
                        "Follow(A) = {b, $}",
                        "Follow(B) = {c, $}",
                        "Follow(S) = {$}"
                ),
                sets("3\nS -> AB\nA -> aA d\nB -> bBc e\n")
        );
    }

    @Test
    void ambiguousSingleNonTerminal() {
        assertEquals(
                List.of(
                        "First(S) = {a}",
                        "Follow(S) = {a, $}"
                ),
                sets("1\nS -> SS a\n")
        );
    }

    @Test
    void chainOfNullableSymbolsStartLineNotFirst() {
        assertEquals(
                List.of(
                        "First(A) = {a, e}",
                        "First(B) = {b, e}",
                        "First(S) = {a, b, c}",
                        "Follow(A) = {b, c}",
                        "Follow(B) = {c}",
                        "Follow(S) = {$}"
                ),
                sets("3\nA -> a e\nS -> ABc\nB -> b e\n")
        );
    }

    @Test
    void nullableStartSymbol() {
        assertEquals(
                List.of(
                        "First(A) = {c, e}",
                        "First(S) = {a, c, e}",
                        "Follow(A) = {b, $}",
                        "Follow(S) = {b, $}"
                ),
                sets("2\nS -> aSb A\nA -> c e\n")
        );
    }
}
