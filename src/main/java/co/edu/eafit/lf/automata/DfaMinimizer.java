package co.edu.eafit.lf.automata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DfaMinimizer {

    private DfaMinimizer() {
    }

    public record Pair(String first, String second) {
    }

    public static final int UNMARKED = -1;

    public record MarkingTable(List<String> states, int[][] round) {

        public boolean marked(int i, int j) {
            return round[Math.max(i, j)][Math.min(i, j)] != UNMARKED;
        }
    }

    public static List<Pair> equivalentPairs(Dfa dfa) {
        return equivalentPairs(markingTable(dfa));
    }

    public static List<Pair> equivalentPairs(MarkingTable table) {
        List<String> states = table.states();
        List<Pair> equivalent = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            for (int j = i + 1; j < states.size(); j++) {
                if (!table.marked(i, j)) {
                    equivalent.add(new Pair(states.get(i), states.get(j)));
                }
            }
        }
        return equivalent;
    }

    public static MarkingTable markingTable(Dfa dfa) {
        List<String> states = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        for (Dfa.State state : dfa.states()) {
            index.put(state.name(), states.size());
            states.add(state.name());
        }
        int n = states.size();
        int[][] round = new int[n][];
        for (int i = 0; i < n; i++) {
            round[i] = new int[i];
            for (int j = 0; j < i; j++) {
                boolean differ = dfa.isAccepting(states.get(i)) != dfa.isAccepting(states.get(j));
                round[i][j] = differ ? 0 : UNMARKED;
            }
        }
        MarkingTable table = new MarkingTable(List.copyOf(states), round);

        boolean changed = true;
        for (int pass = 1; changed; pass++) {
            changed = false;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < i; j++) {
                    if (round[i][j] != UNMARKED) {
                        continue;
                    }
                    for (String symbol : dfa.alphabet()) {
                        String ni = dfa.target(states.get(i), symbol);
                        String nj = dfa.target(states.get(j), symbol);
                        if (leadsToMarked(table, index, ni, nj)) {
                            round[i][j] = pass;
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }
        return table;
    }

    private static boolean leadsToMarked(MarkingTable table, Map<String, Integer> index, String a, String b) {
        if (a == null || b == null) {
            return !Objects.equals(a, b);
        }
        return !a.equals(b) && table.marked(index.get(a), index.get(b));
    }

    public static Dfa minimize(Dfa dfa, List<Pair> equivalentPairs) {
        Map<String, String> parent = new LinkedHashMap<>();
        for (Dfa.State state : dfa.states()) {
            parent.put(state.name(), state.name());
        }
        for (Pair pair : equivalentPairs) {
            union(parent, pair.first(), pair.second());
        }

        Map<String, String> classOf = new LinkedHashMap<>();
        for (String name : parent.keySet()) {
            classOf.put(name, find(parent, name));
        }

        List<Dfa.State> newStates = new ArrayList<>();
        Map<String, Map<String, String>> newTransitions = new LinkedHashMap<>();
        for (Dfa.State state : dfa.states()) {
            String representative = classOf.get(state.name());
            if (!representative.equals(state.name())) {
                continue;
            }
            newStates.add(new Dfa.State(representative, state.subset(), state.accepting()));
            Map<String, String> row = new LinkedHashMap<>();
            for (String symbol : dfa.alphabet()) {
                String target = dfa.target(state.name(), symbol);
                if (target != null) {
                    row.put(symbol, classOf.get(target));
                }
            }
            newTransitions.put(representative, row);
        }

        return new Dfa(classOf.get(dfa.initialState()), newStates, dfa.alphabet(),
                newTransitions, dfa.unreachableNfaStates());
    }

    private static String find(Map<String, String> parent, String x) {
        while (!parent.get(x).equals(x)) {
            x = parent.get(x);
        }
        return x;
    }

    private static void union(Map<String, String> parent, String a, String b) {
        String ra = find(parent, a);
        String rb = find(parent, b);
        if (!ra.equals(rb)) {
            parent.put(ra, rb);
        }
    }
}
