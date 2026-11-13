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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

public final class SLR1Parser implements Parser {

    static final char AUGMENTED_START = '\0';

    public record Item(Production production, int dot) {

        public boolean isComplete() {
            return dot == production.body().size();
        }

        public char nextSymbol() {
            return production.body().get(dot);
        }

        Item advance() {
            return new Item(production, dot + 1);
        }

        @Override
        public String toString() {
            char head = production.head();
            StringBuilder sb = new StringBuilder(head == AUGMENTED_START ? "S'" : String.valueOf(head)).append(" -> ");
            List<Character> body = production.body();
            for (int i = 0; i <= body.size(); i++) {
                if (i == dot) {
                    sb.append('.');
                }
                if (i < body.size()) {
                    sb.append(body.get(i));
                }
            }
            return sb.toString();
        }
    }

    public enum ActionType { SHIFT, REDUCE, ACCEPT }

    public record Action(ActionType type, int state, Production production) {

        static Action shift(int state) {
            return new Action(ActionType.SHIFT, state, null);
        }

        static Action reduce(Production production) {
            return new Action(ActionType.REDUCE, -1, production);
        }

        static Action accept() {
            return new Action(ActionType.ACCEPT, -1, null);
        }

        @Override
        public String toString() {
            return switch (type) {
                case SHIFT -> "s" + state;
                case REDUCE -> "r(" + production + ")";
                case ACCEPT -> "acc";
            };
        }
    }

    private final Grammar grammar;
    private final FirstFollow sets;
    private final Production augmented;
    private final List<Set<Item>> states = new ArrayList<>();
    private final List<Map<Character, Integer>> transitions = new ArrayList<>();
    private final List<Map<Character, Action>> actions = new ArrayList<>();
    private final Set<Cell> conflicts = new LinkedHashSet<>();
    private boolean slr1 = true;

    public SLR1Parser(Grammar grammar, FirstFollow sets) {
        this.grammar = grammar;
        this.sets = sets;
        this.augmented = new Production(AUGMENTED_START, List.of(Grammar.START));
        buildCanonicalCollection();
        buildActionTable();
    }

    public boolean isSLR1() {
        return slr1;
    }

    public List<Set<Item>> states() {
        return Collections.unmodifiableList(states);
    }

    public List<Map<Character, Action>> actionTable() {
        return Collections.unmodifiableList(actions);
    }

    public Map<Character, Integer> transitionsOf(int state) {
        return Collections.unmodifiableMap(transitions.get(state));
    }

    public List<Map<Character, Integer>> gotoTable() {
        List<Map<Character, Integer>> table = new ArrayList<>(transitions.size());
        for (Map<Character, Integer> row : transitions) {
            Map<Character, Integer> gotoRow = new TreeMap<>();
            row.forEach((symbol, target) -> {
                if (Grammar.isNonterminal(symbol)) {
                    gotoRow.put(symbol, target);
                }
            });
            table.add(Collections.unmodifiableMap(gotoRow));
        }
        return Collections.unmodifiableList(table);
    }

    private Set<Item> closure(Set<Item> items) {
        Set<Item> result = new LinkedHashSet<>(items);
        Deque<Item> pending = new ArrayDeque<>(items);
        while (!pending.isEmpty()) {
            Item item = pending.poll();
            if (item.isComplete() || !Grammar.isNonterminal(item.nextSymbol())) {
                continue;
            }
            for (Production p : grammar.productionsOf(item.nextSymbol())) {
                Item added = new Item(p, 0);
                if (result.add(added)) {
                    pending.add(added);
                }
            }
        }
        return result;
    }

    private Set<Item> goTo(Set<Item> items, char symbol) {
        Set<Item> kernel = new LinkedHashSet<>();
        for (Item item : items) {
            if (!item.isComplete() && item.nextSymbol() == symbol) {
                kernel.add(item.advance());
            }
        }
        return closure(kernel);
    }

    private void buildCanonicalCollection() {
        Map<Set<Item>, Integer> index = new HashMap<>();
        addState(closure(Set.of(new Item(augmented, 0))), index);

        for (int i = 0; i < states.size(); i++) {
            SortedSet<Character> symbols = new TreeSet<>();
            for (Item item : states.get(i)) {
                if (!item.isComplete()) {
                    symbols.add(item.nextSymbol());
                }
            }
            for (char symbol : symbols) {
                Set<Item> target = goTo(states.get(i), symbol);
                Integer j = index.get(target);
                if (j == null) {
                    j = addState(target, index);
                }
                transitions.get(i).put(symbol, j);
            }
        }
    }

    private int addState(Set<Item> items, Map<Set<Item>, Integer> index) {
        int id = states.size();
        states.add(items);
        transitions.add(new TreeMap<>());
        index.put(items, id);
        return id;
    }

    private void buildActionTable() {
        for (int i = 0; i < states.size(); i++) {
            Map<Character, Action> row = new TreeMap<>();
            for (Item item : states.get(i)) {
                Production p = item.production();
                if (!item.isComplete()) {
                    char a = item.nextSymbol();
                    if (!Grammar.isNonterminal(a)) {
                        setAction(i, row, a, Action.shift(transitions.get(i).get(a)));
                    }
                } else if (p == augmented) {
                    setAction(i, row, END, Action.accept());
                } else {
                    for (char b : sets.follow(p.head())) {
                        setAction(i, row, b, Action.reduce(p));
                    }
                }
            }
            actions.add(Collections.unmodifiableMap(row));
        }
    }

    private void setAction(int state, Map<Character, Action> row, char symbol, Action action) {
        Action previous = row.putIfAbsent(symbol, action);
        if (previous != null && !previous.equals(action)) {
            slr1 = false;
            conflicts.add(new Cell(state, symbol));
        }
    }

    public record Cell(int state, char terminal) {
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
        return new Trace(List.of("Stack", "Symbols", "Input", "Action"), rows, tree);
    }

    private Optional<ParseNode> run(String input, List<List<String>> rows) {
        if (!slr1) {
            throw new IllegalStateException("Grammar is not SLR(1)");
        }
        String w = (input.equals(String.valueOf(EPSILON)) ? "" : input) + END;
        Deque<Integer> stateStack = new ArrayDeque<>();
        Deque<ParseNode> nodeStack = new ArrayDeque<>();
        stateStack.push(0);

        int ip = 0;
        while (true) {
            char a = w.charAt(ip);
            Action action = actions.get(stateStack.peek()).get(a);
            record(rows, w, ip, stateStack, nodeStack, action);
            if (action == null) {
                return Optional.empty();
            }
            switch (action.type()) {
                case SHIFT -> {
                    stateStack.push(action.state());
                    nodeStack.push(new ParseNode(a));
                    ip++;
                }
                case REDUCE -> {
                    Production p = action.production();
                    ParseNode node = new ParseNode(p.head());
                    node.expand(p, popChildren(p, stateStack, nodeStack));
                    nodeStack.push(node);
                    stateStack.push(transitions.get(stateStack.peek()).get(p.head()));
                }
                case ACCEPT -> {
                    return ip == w.length() - 1 ? Optional.of(nodeStack.peek()) : Optional.empty();
                }
            }
        }
    }

    private static void record(List<List<String>> rows, String w, int ip, Deque<Integer> stateStack,
                               Deque<ParseNode> nodeStack, Action action) {
        if (rows == null) {
            return;
        }
        StringJoiner states = new StringJoiner(" ");
        stateStack.descendingIterator().forEachRemaining(s -> states.add(String.valueOf(s)));
        StringBuilder symbols = new StringBuilder();
        nodeStack.descendingIterator().forEachRemaining(node -> symbols.append(node.symbol()));
        String text = action == null ? "error" : switch (action.type()) {
            case SHIFT -> "shift " + action.state();
            case REDUCE -> "reduce by " + action.production();
            case ACCEPT -> w.length() - 1 == ip ? "accept" : "error";
        };
        rows.add(List.of(states.toString(), symbols.toString(), w.substring(ip), text));
    }

    private static List<ParseNode> popChildren(Production p, Deque<Integer> stateStack, Deque<ParseNode> nodeStack) {
        if (p.isEpsilon()) {
            return List.of(new ParseNode(EPSILON));
        }
        ParseNode[] children = new ParseNode[p.body().size()];
        for (int k = children.length - 1; k >= 0; k--) {
            stateStack.pop();
            children[k] = nodeStack.pop();
        }
        return List.of(children);
    }
}
