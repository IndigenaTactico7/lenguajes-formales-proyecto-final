package co.edu.eafit.lf.automata;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Nfa(
        Set<Integer> states,
        List<String> alphabet,
        int initial,
        Set<Integer> accepting,
        Map<Integer, Map<String, Set<Integer>>> delta
) {

    public static final String EPSILON = "";

    public Set<Integer> targets(int state, String symbol) {
        return delta.getOrDefault(state, Map.of()).getOrDefault(symbol, Set.of());
    }
}
