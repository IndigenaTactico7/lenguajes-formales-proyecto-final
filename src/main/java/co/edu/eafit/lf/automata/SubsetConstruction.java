package co.edu.eafit.lf.automata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class SubsetConstruction {

    private SubsetConstruction() {
    }

    public static SortedSet<Integer> epsilonClosure(Nfa nfa, Collection<Integer> states) {
        SortedSet<Integer> closure = new TreeSet<>(states);
        Deque<Integer> stack = new ArrayDeque<>(states);
        while (!stack.isEmpty()) {
            int state = stack.pop();
            for (int next : nfa.targets(state, Nfa.EPSILON)) {
                if (closure.add(next)) {
                    stack.push(next);
                }
            }
        }
        return closure;
    }

    public static SortedSet<Integer> move(Nfa nfa, Collection<Integer> states, String symbol) {
        SortedSet<Integer> result = new TreeSet<>();
        for (int state : states) {
            result.addAll(nfa.targets(state, symbol));
        }
        return result;
    }

    public static Dfa convert(Nfa nfa) {
        validate(nfa);

        String separator = separatorFor(nfa);

        Map<SortedSet<Integer>, String> names = new LinkedHashMap<>();
        Deque<SortedSet<Integer>> pending = new ArrayDeque<>();

        SortedSet<Integer> start = epsilonClosure(nfa, Set.of(nfa.initial()));
        names.put(start, name(start, separator));
        pending.add(start);

        Map<String, Map<String, String>> transitions = new LinkedHashMap<>();
        while (!pending.isEmpty()) {
            SortedSet<Integer> current = pending.poll();
            Map<String, String> row = new LinkedHashMap<>();
            for (String symbol : nfa.alphabet()) {
                SortedSet<Integer> next = epsilonClosure(nfa, move(nfa, current, symbol));
                if (next.isEmpty()) {
                    continue;
                }
                if (!names.containsKey(next)) {
                    names.put(next, name(next, separator));
                    pending.add(next);
                }
                row.put(symbol, names.get(next));
            }
            transitions.put(names.get(current), row);
        }

        List<Dfa.State> states = new ArrayList<>();
        Set<Integer> covered = new LinkedHashSet<>();
        for (Map.Entry<SortedSet<Integer>, String> entry : names.entrySet()) {
            SortedSet<Integer> subset = entry.getKey();
            covered.addAll(subset);
            states.add(new Dfa.State(entry.getValue(), List.copyOf(subset), accepts(nfa, subset)));
        }

        List<Integer> unreachable = new ArrayList<>();
        for (int state : nfa.states()) {
            if (!covered.contains(state)) {
                unreachable.add(state);
            }
        }
        Collections.sort(unreachable);

        return new Dfa(names.get(start), states, nfa.alphabet(), transitions, unreachable);
    }

    private static boolean accepts(Nfa nfa, Set<Integer> subset) {
        for (int state : subset) {
            if (nfa.accepting().contains(state)) {
                return true;
            }
        }
        return false;
    }

    private static String name(SortedSet<Integer> subset, String separator) {
        StringBuilder name = new StringBuilder();
        for (int state : subset) {
            if (!name.isEmpty()) {
                name.append(separator);
            }
            name.append(state);
        }
        return name.toString();
    }

    private static String separatorFor(Nfa nfa) {
        for (int state : nfa.states()) {
            if (state < 0 || state > 9) {
                return ",";
            }
        }
        return "";
    }

    private static void validate(Nfa nfa) {
        if (nfa.alphabet().isEmpty()) {
            throw new IllegalArgumentException("The alphabet is empty.");
        }
        if (nfa.alphabet().contains(Nfa.EPSILON)) {
            throw new IllegalArgumentException("The alphabet cannot contain the empty string: epsilon is "
                    + "a transition label, not an input symbol.");
        }
        if (!nfa.states().contains(nfa.initial())) {
            throw new IllegalArgumentException("The initial state " + nfa.initial() + " is not part of the states.");
        }
    }
}
