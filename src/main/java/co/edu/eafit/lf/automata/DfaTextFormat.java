package co.edu.eafit.lf.automata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public final class DfaTextFormat {

    private DfaTextFormat() {
    }

    public static List<Dfa> parseCases(String text) {
        Scanner scanner = new Scanner(text);
        int cases = Integer.parseInt(nextNonBlankLine(scanner));
        List<Dfa> result = new ArrayList<>();

        for (int c = 0; c < cases; c++) {
            int n = Integer.parseInt(nextNonBlankLine(scanner));
            List<String> alphabet = List.of(nextNonBlankLine(scanner).trim().split("\\s+"));

            Set<Integer> finalStates = new LinkedHashSet<>();
            String finalsLine = nextNonBlankLine(scanner).trim();
            if (!finalsLine.isEmpty()) {
                for (String token : finalsLine.split("\\s+")) {
                    finalStates.add(Integer.parseInt(token));
                }
            }

            List<Dfa.State> states = new ArrayList<>();
            Map<String, Map<String, String>> transitions = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                String[] row = nextNonBlankLine(scanner).trim().split("\\s+");
                int offset = row.length - alphabet.size();
                Map<String, String> bySymbol = new LinkedHashMap<>();
                for (int j = 0; j < alphabet.size(); j++) {
                    bySymbol.put(alphabet.get(j), row[offset + j]);
                }
                transitions.put(String.valueOf(i), bySymbol);
                states.add(new Dfa.State(String.valueOf(i), List.of(i), finalStates.contains(i)));
            }

            result.add(new Dfa("0", states, alphabet, transitions, List.of()));
        }
        return result;
    }

    private static String nextNonBlankLine(Scanner scanner) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.isBlank()) {
                return line;
            }
        }
        throw new IllegalArgumentException("Unexpected end of input while reading a case.");
    }

    public static String formatPairs(List<DfaMinimizer.Pair> pairs) {
        List<DfaMinimizer.Pair> sorted = new ArrayList<>(pairs);
        sorted.sort(Comparator.comparing(DfaMinimizer.Pair::first).thenComparing(DfaMinimizer.Pair::second));

        StringBuilder line = new StringBuilder();
        for (DfaMinimizer.Pair pair : sorted) {
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append('(').append(pair.first()).append(',').append(pair.second()).append(')');
        }
        return line.toString();
    }
}
