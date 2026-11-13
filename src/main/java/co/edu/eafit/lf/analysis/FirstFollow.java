package co.edu.eafit.lf.analysis;

import static co.edu.eafit.lf.grammar.Grammar.END;
import static co.edu.eafit.lf.grammar.Grammar.EPSILON;

import co.edu.eafit.lf.grammar.Grammar;
import co.edu.eafit.lf.grammar.Production;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

public final class FirstFollow {

    private final Grammar grammar;
    private final Map<Character, Set<Character>> first = new HashMap<>();
    private final Map<Character, Set<Character>> follow = new HashMap<>();

    public FirstFollow(Grammar grammar) {
        this.grammar = grammar;
        computeFirst();
        computeFollow();
    }

    public Set<Character> first(char symbol) {
        if (!Grammar.isNonterminal(symbol)) {
            return Set.of(symbol);
        }
        return Collections.unmodifiableSet(first.getOrDefault(symbol, Set.of()));
    }

    public Set<Character> firstOf(List<Character> symbols) {
        return firstOf(symbols, 0);
    }

    public Set<Character> follow(char nonterminal) {
        return Collections.unmodifiableSet(follow.getOrDefault(nonterminal, Set.of()));
    }

    private Set<Character> firstOf(List<Character> symbols, int from) {
        Set<Character> result = new HashSet<>();
        for (int i = from; i < symbols.size(); i++) {
            Set<Character> symbolFirst = first(symbols.get(i));
            for (char c : symbolFirst) {
                if (c != EPSILON) {
                    result.add(c);
                }
            }
            if (!symbolFirst.contains(EPSILON)) {
                return result;
            }
        }
        result.add(EPSILON);
        return result;
    }

    private void computeFirst() {
        grammar.nonterminals().forEach(a -> first.put(a, new HashSet<>()));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production p : grammar.productions()) {
                changed |= first.get(p.head()).addAll(firstOf(p.body()));
            }
        }
    }

    private void computeFollow() {
        grammar.nonterminals().forEach(a -> follow.put(a, new HashSet<>()));
        follow.computeIfAbsent(Grammar.START, k -> new HashSet<>()).add(END);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production p : grammar.productions()) {
                List<Character> body = p.body();
                for (int i = 0; i < body.size(); i++) {
                    char b = body.get(i);
                    if (!Grammar.isNonterminal(b)) {
                        continue;
                    }
                    Set<Character> rest = firstOf(body, i + 1);
                    Set<Character> target = follow.get(b);
                    for (char c : rest) {
                        if (c != EPSILON) {
                            changed |= target.add(c);
                        }
                    }
                    if (rest.contains(EPSILON)) {
                        changed |= target.addAll(follow.get(p.head()));
                    }
                }
            }
        }
    }

    public String formatSets() {
        List<String> lines = new ArrayList<>();
        grammar.nonterminals().forEach(a -> lines.add("First(" + a + ") = " + format(first(a))));
        grammar.nonterminals().forEach(a -> lines.add("Follow(" + a + ") = " + format(follow(a))));
        return String.join("\n", lines);
    }

    public static String format(Set<Character> set) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        new TreeSet<>(set).stream()
                .filter(c -> c != EPSILON && c != END)
                .forEach(c -> joiner.add(String.valueOf(c)));
        if (set.contains(EPSILON)) {
            joiner.add(String.valueOf(EPSILON));
        }
        if (set.contains(END)) {
            joiner.add(String.valueOf(END));
        }
        return joiner.toString();
    }
}
