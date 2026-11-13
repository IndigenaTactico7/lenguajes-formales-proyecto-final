package co.edu.eafit.lf.parsing;

import static co.edu.eafit.lf.grammar.Grammar.END;
import static co.edu.eafit.lf.grammar.Grammar.EPSILON;

import co.edu.eafit.lf.analysis.FirstFollow;
import co.edu.eafit.lf.grammar.Grammar;
import co.edu.eafit.lf.grammar.Production;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class LL1Parser implements Parser {

    private final Grammar grammar;
    private final FirstFollow sets;
    private final boolean ll1;
    private final Set<Cell> conflicts = new LinkedHashSet<>();
    private final Map<Character, Map<Character, Production>> table;

    public LL1Parser(Grammar grammar, FirstFollow sets) {
        this.grammar = grammar;
        this.sets = sets;
        this.ll1 = satisfiesLL1Condition();
        this.table = buildTable();
    }

    public boolean isLL1() {
        return ll1;
    }

    public Map<Character, Map<Character, Production>> table() {
        return table;
    }

    private boolean satisfiesLL1Condition() {
        for (char head : grammar.nonterminals()) {
            List<Production> alternatives = grammar.productionsOf(head);
            Set<Character> follow = sets.follow(head);
            for (int i = 0; i < alternatives.size(); i++) {
                Set<Character> alpha = sets.firstOf(alternatives.get(i).body());
                for (int j = i + 1; j < alternatives.size(); j++) {
                    Set<Character> beta = sets.firstOf(alternatives.get(j).body());
                    boolean alphaNullable = alpha.contains(EPSILON);
                    boolean betaNullable = beta.contains(EPSILON);
                    if (sharesTerminal(alpha, beta)
                            || (alphaNullable && betaNullable)
                            || (betaNullable && sharesTerminal(alpha, follow))
                            || (alphaNullable && sharesTerminal(beta, follow))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean sharesTerminal(Set<Character> a, Set<Character> b) {
        Set<Character> common = new HashSet<>(a);
        common.retainAll(b);
        common.remove(EPSILON);
        return !common.isEmpty();
    }

    private Map<Character, Map<Character, Production>> buildTable() {
        Map<Character, Map<Character, Production>> m = new TreeMap<>();
        for (Production p : grammar.productions()) {
            Map<Character, Production> row = m.computeIfAbsent(p.head(), k -> new TreeMap<>());
            Set<Character> first = sets.firstOf(p.body());
            for (char a : first) {
                if (a != EPSILON) {
                    put(row, p, a);
                }
            }
            if (first.contains(EPSILON)) {
                for (char b : sets.follow(p.head())) {
                    put(row, p, b);
                }
            }
        }
        m.replaceAll((head, row) -> Collections.unmodifiableMap(row));
        return Collections.unmodifiableMap(m);
    }

    private void put(Map<Character, Production> row, Production p, char terminal) {
        Production previous = row.putIfAbsent(terminal, p);
        if (previous != null && !previous.equals(p)) {
            conflicts.add(new Cell(p.head(), terminal));
        }
    }

    public record Cell(char nonterminal, char terminal) {
    }

    public Set<Cell> conflicts() {
        return Collections.unmodifiableSet(conflicts);
    }

    @Override
    public Optional<ParseNode> parse(String input) {
        return run(input, null);
    }

    @Override
    public Trace trace(String input) {
        List<List<String>> rows = new ArrayList<>();
        Optional<ParseNode> tree = run(input, rows);
        return new Trace(List.of("Matched", "Stack", "Input", "Action"), rows, tree);
    }

    private Optional<ParseNode> run(String input, List<List<String>> rows) {
        if (!ll1) {
            throw new IllegalStateException("Grammar is not LL(1)");
        }
        String w = (input.equals(String.valueOf(EPSILON)) ? "" : input) + END;
        ParseNode root = new ParseNode(Grammar.START);
        Deque<ParseNode> stack = new ArrayDeque<>();
        stack.push(new ParseNode(END));
        stack.push(root);

        int ip = 0;
        while (stack.peek().symbol() != END) {
            char x = stack.peek().symbol();
            char a = w.charAt(ip);
            if (!Grammar.isNonterminal(x)) {
                if (x != a) {
                    record(rows, w, ip, stack, "error");
                    return Optional.empty();
                }
                record(rows, w, ip, stack, "match " + a);
                stack.pop();
                ip++;
                continue;
            }
            Production p = table.getOrDefault(x, Map.of()).get(a);
            if (p == null) {
                record(rows, w, ip, stack, "error");
                return Optional.empty();
            }
            record(rows, w, ip, stack, "output " + p);
            ParseNode top = stack.pop();
            top.expand(p, childrenOf(p));
            if (!p.isEpsilon()) {
                List<ParseNode> children = top.children();
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
            }
        }
        boolean accepted = ip == w.length() - 1;
        record(rows, w, ip, stack, accepted ? "accept" : "error");
        return accepted ? Optional.of(root) : Optional.empty();
    }

    private static void record(List<List<String>> rows, String w, int ip, Deque<ParseNode> stack, String action) {
        if (rows == null) {
            return;
        }
        StringBuilder stackText = new StringBuilder();
        stack.forEach(node -> stackText.append(node.symbol()));
        rows.add(List.of(w.substring(0, ip), stackText.toString(), w.substring(ip), action));
    }

    private static List<ParseNode> childrenOf(Production p) {
        if (p.isEpsilon()) {
            return List.of(new ParseNode(EPSILON));
        }
        List<ParseNode> children = new ArrayList<>(p.body().size());
        p.body().forEach(symbol -> children.add(new ParseNode(symbol)));
        return children;
    }
}
