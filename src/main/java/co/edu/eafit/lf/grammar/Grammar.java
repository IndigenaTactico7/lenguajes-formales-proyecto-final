package co.edu.eafit.lf.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public final class Grammar {

    public static final char START = 'S';
    public static final char EPSILON = 'e';
    public static final char END = '$';

    private final List<Production> productions;
    private final Map<Character, List<Production>> byHead;
    private final SortedSet<Character> nonterminals;
    private final SortedSet<Character> terminals;

    public Grammar(List<Production> productions) {
        this.productions = List.copyOf(productions);

        Map<Character, List<Production>> grouped = new LinkedHashMap<>();
        SortedSet<Character> terms = new TreeSet<>();
        for (Production p : this.productions) {
            grouped.computeIfAbsent(p.head(), k -> new ArrayList<>()).add(p);
            for (char symbol : p.body()) {
                if (!isNonterminal(symbol)) {
                    terms.add(symbol);
                }
            }
        }
        grouped.replaceAll((head, list) -> List.copyOf(list));

        this.byHead = Collections.unmodifiableMap(grouped);
        this.nonterminals = Collections.unmodifiableSortedSet(new TreeSet<>(grouped.keySet()));
        this.terminals = Collections.unmodifiableSortedSet(terms);
    }

    public static boolean isNonterminal(char symbol) {
        return Character.isUpperCase(symbol);
    }

    public List<Production> productions() {
        return productions;
    }

    public List<Production> productionsOf(char nonterminal) {
        return byHead.getOrDefault(nonterminal, List.of());
    }

    public SortedSet<Character> nonterminals() {
        return nonterminals;
    }

    public SortedSet<Character> terminals() {
        return terminals;
    }
}
