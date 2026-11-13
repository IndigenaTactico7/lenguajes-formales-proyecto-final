package co.edu.eafit.lf.web;

import java.util.List;

public final class AutomataModels {

    private AutomataModels() {
    }

    public record Transition(Integer from, String symbol, Integer to) {
        public Transition {
            symbol = symbol == null || symbol.equals("ε") ? "" : symbol;
        }
    }

    public record NfaRequest(List<Integer> states, List<String> alphabet, Integer initial,
                             List<Integer> accepting, List<Transition> transitions) {
        public NfaRequest {
            states = states == null ? List.of() : states;
            alphabet = alphabet == null ? List.of() : alphabet;
            accepting = accepting == null ? List.of() : accepting;
            transitions = transitions == null ? List.of() : transitions;
        }
    }

    public record DfaTransition(String from, String symbol, String to) {
    }

    public record DfaState(String name, List<Integer> subset, boolean accepting) {
    }

    public record DfaResponse(List<String> dfaStates, String initialState, List<String> alphabet,
                              List<DfaTransition> transitions, List<String> acceptingStates,
                              List<Integer> unreachableNfaStates, List<DfaState> stateDetails) {
        public DfaResponse {
            dfaStates = dfaStates == null ? List.of() : dfaStates;
            alphabet = alphabet == null ? List.of() : alphabet;
            transitions = transitions == null ? List.of() : transitions;
            acceptingStates = acceptingStates == null ? List.of() : acceptingStates;
            unreachableNfaStates = unreachableNfaStates == null ? List.of() : unreachableNfaStates;
            stateDetails = stateDetails == null ? List.of() : stateDetails;
        }
    }

    public record SimulateRequest(DfaResponse dfa, String input) {
        public SimulateRequest {
            input = input == null ? "" : input;
        }
    }

    public record SimulationResponse(List<String> path, boolean accepted, String message) {
    }

    public record Pair(String first, String second) {
    }

    public record MarkingTable(List<String> states, List<List<Integer>> rounds) {
    }

    public record MinimizeResponse(List<Pair> equivalentStates, DfaResponse minimizedDfa, MarkingTable markingTable) {
    }
}
