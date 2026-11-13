package co.edu.eafit.lf.automata;

import java.util.List;
import java.util.Map;

public record Dfa(
        String initialState,
        List<State> states,
        List<String> alphabet,
        Map<String, Map<String, String>> transitions,
        List<Integer> unreachableNfaStates
) {

    public record State(String name, List<Integer> subset, boolean accepting) {
    }

    public String target(String state, String symbol) {
        return transitions.getOrDefault(state, Map.of()).get(symbol);
    }

    public boolean isAccepting(String state) {
        if (states == null || state == null) {
            return false;
        }
        for (State candidate : states) {
            if (candidate.name().equals(state)) {
                return candidate.accepting();
            }
        }
        return false;
    }
}
