package co.edu.eafit.lf.cli;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleRunnerTest {

    private static final String MENU = "Select a parser (T: for LL(1), B: for SLR(1), Q: quit):";

    private static List<String> run(String input, boolean printSets) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(output);
            new ConsoleRunner().session(new BufferedReader(new StringReader(input)), printStream, printSets);
            printStream.flush();
            return output.toString().lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void slr1GrammarSession() {
        String input = "3\nS -> S+T T\nT -> T*F F\nF -> (S) i\ni+i\n(i)\n(i+i)*i)\n\n";
        assertEquals(
                List.of("Grammar is SLR(1).", "yes", "yes", "no"),
                run(input, false)
        );
    }

    @Test
    void bothParsersSelectLL1() {
        String input = "3\nS -> AB\nA -> aA d\nB -> bBc e\nT\nd\nadbc\na\n\nQ\n";
        assertEquals(
                List.of(MENU, "yes", "yes", "no", MENU),
                run(input, false)
        );
    }

    @Test
    void bothParsersSelectSLR1ThenLL1() {
        String input = "3\nS -> AB\nA -> aA d\nB -> bBc e\nB\nadbc\n\nT\na\n\nQ\n";
        assertEquals(
                List.of(MENU, "yes", MENU, "no", MENU),
                run(input, false)
        );
    }

    @Test
    void grammarIsNeitherLL1NorSLR1() {
        String input = "1\nS -> SS a\n";
        assertEquals(
                List.of("Grammar is neither LL(1) nor SLR(1)."),
                run(input, false)
        );
    }

    @Test
    void ll1GrammarSession() {
        String input = "3\nS -> AaAb BbBa\nA -> e\nB -> e\nab\nba\naa\n\n";
        assertEquals(
                List.of("Grammar is LL(1).", "yes", "yes", "no"),
                run(input, false)
        );
    }

    @Test
    void neitherLL1NorSLR1WithPrintSets() {
        String input = "1\nS -> SS a\n";
        assertEquals(
                List.of("First(S) = {a}", "Follow(S) = {a, $}", "Grammar is neither LL(1) nor SLR(1)."),
                run(input, true)
        );
    }
}
