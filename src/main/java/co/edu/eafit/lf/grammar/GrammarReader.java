package co.edu.eafit.lf.grammar;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GrammarReader {

    private static final String ARROW = "->";

    private GrammarReader() {
    }

    public static Grammar read(BufferedReader in) throws IOException {
        int count = parseCount(requireLine(in));
        List<Production> productions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            parseLine(requireLine(in), productions);
        }
        return new Grammar(productions);
    }

    private static int parseCount(String line) {
        try {
            return Integer.parseInt(line.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected the number of nonterminals, got: " + line, e);
        }
    }

    private static void parseLine(String line, List<Production> productions) {
        int arrow = line.indexOf(ARROW);
        if (arrow < 0) {
            throw new IllegalArgumentException("Missing '->' in production line: " + line);
        }
        String headText = line.substring(0, arrow).trim();
        if (headText.length() != 1 || !Grammar.isNonterminal(headText.charAt(0))) {
            throw new IllegalArgumentException("Invalid nonterminal in line: " + line);
        }
        char head = headText.charAt(0);

        for (String alternative : line.substring(arrow + ARROW.length()).trim().split("\\s+")) {
            productions.add(new Production(head, toBody(alternative)));
        }
    }

    private static List<Character> toBody(String alternative) {
        if (alternative.equals(String.valueOf(Grammar.EPSILON))) {
            return List.of();
        }
        List<Character> body = new ArrayList<>(alternative.length());
        for (char symbol : alternative.toCharArray()) {
            body.add(symbol);
        }
        return body;
    }

    private static String requireLine(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            throw new IllegalArgumentException("Unexpected end of input while reading the grammar");
        }
        return line;
    }
}
