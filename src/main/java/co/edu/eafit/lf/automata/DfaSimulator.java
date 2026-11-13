package co.edu.eafit.lf.automata;

import java.util.ArrayList;
import java.util.List;

public final class DfaSimulator {

    private DfaSimulator() {
    }

    public record Result(List<String> path, boolean accepted, String message) {
    }

    public static Result simulate(Dfa dfa, String input) {
        String current = dfa.initialState();
        List<String> path = new ArrayList<>();
        path.add(current);

        for (int i = 0; i < input.length(); i++) {
            String symbol = String.valueOf(input.charAt(i));
            String next = dfa.target(current, symbol);
            if (next == null) {
                return new Result(path, false,
                        "No transition from state '" + current + "' on symbol '" + symbol + "'.");
            }
            path.add(next);
            current = next;
        }

        if (!dfa.isAccepting(current)) {
            return new Result(path, false,
                    "The input was fully consumed but state '" + current + "' is not accepting.");
        }
        return new Result(path, true, null);
    }
}
