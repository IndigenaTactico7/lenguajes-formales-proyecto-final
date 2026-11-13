package co.edu.eafit.lf.automata;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomataTest {

    private static void addTransition(Map<Integer, Map<String, Set<Integer>>> delta, int from, String symbol, int to) {
        delta.computeIfAbsent(from, k -> new LinkedHashMap<>())
                .computeIfAbsent(symbol, k -> new LinkedHashSet<>())
                .add(to);
    }

    private static Nfa exampleNfa() {
        Set<Integer> states = new LinkedHashSet<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
        List<String> alphabet = List.of("a", "b");
        int initial = 0;
        Set<Integer> accepting = new LinkedHashSet<>(List.of(2, 6, 8));

        Map<Integer, Map<String, Set<Integer>>> delta = new LinkedHashMap<>();
        addTransition(delta, 0, "", 1);
        addTransition(delta, 0, "", 3);
        addTransition(delta, 0, "", 7);
        addTransition(delta, 1, "a", 2);
        addTransition(delta, 3, "a", 4);
        addTransition(delta, 4, "b", 5);
        addTransition(delta, 5, "b", 6);
        addTransition(delta, 7, "a", 7);
        addTransition(delta, 7, "b", 8);
        addTransition(delta, 8, "b", 8);

        return new Nfa(states, alphabet, initial, accepting, delta);
    }

    @Test
    void subsetConstructionOfExample() {
        Nfa nfa = exampleNfa();
        Dfa dfa = SubsetConstruction.convert(nfa);

        assertEquals("0137", dfa.initialState());

        List<String> stateNames = dfa.states().stream().map(Dfa.State::name).toList();
        assertEquals(List.of("0137", "247", "8", "7", "58", "68"), stateNames);

        Set<String> acceptingNames = dfa.states().stream()
                .filter(Dfa.State::accepting)
                .map(Dfa.State::name)
                .collect(Collectors.toSet());
        assertEquals(Set.of("247", "8", "58", "68"), acceptingNames);

        assertEquals("247", dfa.target("0137", "a"));
        assertEquals("8", dfa.target("0137", "b"));
        assertEquals("7", dfa.target("247", "a"));
        assertEquals("58", dfa.target("247", "b"));
        assertEquals("68", dfa.target("58", "b"));
        assertEquals("8", dfa.target("68", "b"));
        assertEquals("7", dfa.target("7", "a"));
        assertEquals("8", dfa.target("7", "b"));
        assertEquals("8", dfa.target("8", "b"));
    }

    @Test
    void simulationAcceptsAndRejects() {
        Nfa nfa = exampleNfa();
        Dfa dfa = SubsetConstruction.convert(nfa);

        DfaSimulator.Result abbResult = DfaSimulator.simulate(dfa, "abb");
        assertTrue(abbResult.accepted());
        assertEquals(List.of("0137", "247", "58", "68"), abbResult.path());

        DfaSimulator.Result aaResult = DfaSimulator.simulate(dfa, "aa");
        assertFalse(aaResult.accepted());
        assertEquals("7", aaResult.path().getLast());

        DfaSimulator.Result baResult = DfaSimulator.simulate(dfa, "ba");
        assertFalse(baResult.accepted());
        assertNotNull(baResult.message());
    }

    @Test
    void minimizationFindsEquivalentPair() {
        List<String> alphabet = List.of("a", "b");
        Set<String> accepting = Set.of("1", "4", "5");

        List<Dfa.State> states = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            String name = String.valueOf(i);
            states.add(new Dfa.State(name, List.of(), accepting.contains(name)));
        }

        Map<String, Map<String, String>> transitions = new LinkedHashMap<>();
        transitions.put("0", Map.of("a", "1", "b", "2"));
        transitions.put("1", Map.of("a", "3", "b", "4"));
        transitions.put("2", Map.of("a", "4", "b", "3"));
        transitions.put("3", Map.of("a", "5", "b", "5"));
        transitions.put("4", Map.of("a", "5", "b", "5"));
        transitions.put("5", Map.of("a", "5", "b", "5"));

        Dfa dfa = new Dfa("0", states, alphabet, transitions, List.of());

        DfaMinimizer.MarkingTable table = DfaMinimizer.markingTable(dfa);
        assertEquals(0, table.round()[1][0]);                      
        assertEquals(DfaMinimizer.UNMARKED, table.round()[5][4]);  

        List<DfaMinimizer.Pair> pairs = DfaMinimizer.equivalentPairs(dfa);
        assertEquals(List.of(new DfaMinimizer.Pair("4", "5")), pairs);

        Dfa minimized = DfaMinimizer.minimize(dfa, pairs);
        assertEquals(5, minimized.states().size());
    }

    @Test
    void batchFormat() {
        String input = "1\n6\na b\n1 4 5\n0 1 2\n1 3 4\n2 4 3\n3 5 5\n4 5 5\n5 5 5\n";
        List<Dfa> cases = DfaTextFormat.parseCases(input);
        assertEquals(1, cases.size());

        Dfa dfa = cases.getFirst();
        assertEquals("(4,5)", DfaTextFormat.formatPairs(DfaMinimizer.equivalentPairs(dfa)));
    }

    @Test
    void invalidNfaIsRejected() {
        Nfa emptyAlphabet = new Nfa(Set.of(0), List.of(), 0, Set.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> SubsetConstruction.convert(emptyAlphabet));

        Nfa invalidInitial = new Nfa(Set.of(1, 2), List.of("a"), 0, Set.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> SubsetConstruction.convert(invalidInitial));
    }
}
