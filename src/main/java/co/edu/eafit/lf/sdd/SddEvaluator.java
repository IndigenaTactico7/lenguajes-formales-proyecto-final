package co.edu.eafit.lf.sdd;

import co.edu.eafit.lf.analysis.FirstFollow;
import co.edu.eafit.lf.grammar.Grammar;
import co.edu.eafit.lf.grammar.GrammarReader;
import co.edu.eafit.lf.parsing.LL1Parser;
import co.edu.eafit.lf.parsing.ParseNode;
import co.edu.eafit.lf.parsing.Parser;
import co.edu.eafit.lf.parsing.SLR1Parser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;

public final class SddEvaluator {

    public static final String LEXVAL = "lexval";

    public static final class CircularSddException extends RuntimeException {
        CircularSddException(int pending) {
            super("The SDD is circular: " + pending + " attribute instances are part of or depend on a cycle");
        }
    }

    public record Instance(int nodeId, char symbol, String attribute) {
        @Override
        public String toString() {
            return symbol + "." + attribute + " (node " + nodeId + ")";
        }
    }

    public record Evaluation(ParseNode tree, Map<ParseNode, Integer> ids, List<Instance> order,
                             Map<Instance, List<Instance>> dependencies, Object mainValue) {

        public String formatTree() {
            StringJoiner out = new StringJoiner("\n");
            Deque<ParseNode> nodes = new ArrayDeque<>();
            Deque<Integer> depths = new ArrayDeque<>();
            nodes.push(tree);
            depths.push(0);
            while (!nodes.isEmpty()) {
                ParseNode node = nodes.pop();
                int depth = depths.pop();
                StringBuilder line = new StringBuilder("  ".repeat(depth))
                        .append('[').append(ids.get(node)).append("] ").append(node.symbol());
                if (!node.attributes().isEmpty()) {
                    StringJoiner attrs = new StringJoiner(", ", "  ", "");
                    new TreeMap<>(node.attributes()).forEach((name, value) -> attrs.add(name + "=" + value));
                    line.append(attrs);
                }
                out.add(line);
                List<ParseNode> children = node.children();
                for (int i = children.size() - 1; i >= 0; i--) {
                    nodes.push(children.get(i));
                    depths.push(depth + 1);
                }
            }
            return out.toString();
        }

        public String formatOrder() {
            StringJoiner out = new StringJoiner("\n");
            for (int i = 0; i < order.size(); i++) {
                out.add((i + 1) + ". " + order.get(i));
            }
            return out.toString();
        }
    }

    private record Binding(Sdd.Rule rule, List<Instance> arguments) {
    }

    private final Sdd sdd;
    private final Parser parser;

    public SddEvaluator(Sdd sdd) {
        this.sdd = sdd;
        this.parser = parserFor(readGrammar(sdd.grammarText()));
    }

    private static Grammar readGrammar(String text) {
        try {
            return GrammarReader.read(new BufferedReader(new StringReader(text)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Parser parserFor(Grammar grammar) {
        FirstFollow sets = new FirstFollow(grammar);
        LL1Parser ll1 = new LL1Parser(grammar, sets);
        if (ll1.isLL1()) {
            return ll1;
        }
        SLR1Parser slr1 = new SLR1Parser(grammar, sets);
        if (slr1.isSLR1()) {
            return slr1;
        }
        throw new IllegalArgumentException("The grammar of the SDD is neither LL(1) nor SLR(1)");
    }

    public Optional<Evaluation> evaluate(String input) {
        return parser.parse(input).map(this::evaluateTree);
    }

    private Evaluation evaluateTree(ParseNode root) {
        Map<ParseNode, Integer> ids = numberNodes(root);
        Map<Instance, ParseNode> owner = new HashMap<>();
        Map<Instance, Binding> definitions = new HashMap<>();
        Map<Instance, List<Instance>> dependencies = new LinkedHashMap<>();

        buildDependencyGraph(root, ids, owner, definitions, dependencies);
        List<Instance> order = topologicalOrder(dependencies);

        for (Instance instance : order) {
            Binding binding = definitions.get(instance);
            if (binding == null) {
                continue; 
            }
            List<Object> values = new ArrayList<>(binding.arguments().size());
            for (Instance argument : binding.arguments()) {
                values.add(owner.get(argument).attribute(argument.attribute()));
            }
            owner.get(instance).setAttribute(instance.attribute(), binding.rule().function().apply(values));
        }
        return new Evaluation(root, Collections.unmodifiableMap(ids), List.copyOf(order),
                Collections.unmodifiableMap(dependencies), root.attribute(sdd.mainAttribute()));
    }

    private static Map<ParseNode, Integer> numberNodes(ParseNode root) {
        Map<ParseNode, Integer> ids = new IdentityHashMap<>();
        Deque<ParseNode> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            ParseNode node = pending.pop();
            ids.put(node, ids.size() + 1);
            List<ParseNode> children = node.children();
            for (int i = children.size() - 1; i >= 0; i--) {
                pending.push(children.get(i));
            }
        }
        return ids;
    }

    private void buildDependencyGraph(ParseNode root, Map<ParseNode, Integer> ids, Map<Instance, ParseNode> owner,
                                      Map<Instance, Binding> definitions, Map<Instance, List<Instance>> dependencies) {
        Deque<ParseNode> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            ParseNode node = pending.pop();
            if (node.isLeaf()) {
                if (node.symbol() != Grammar.EPSILON) {
                    node.setAttribute(LEXVAL, node.symbol());
                    Instance lexval = instance(node, LEXVAL, ids, owner);
                    dependencies.put(lexval, List.of());
                }
                continue;
            }
            List<Sdd.Rule> rules = sdd.rules().get(node.production());
            if (rules == null) {
                throw new IllegalStateException("No semantic rules for production " + node.production());
            }
            for (Sdd.Rule rule : rules) {
                Instance target = instance(at(node, rule.target()), rule.target().name(), ids, owner);
                List<Instance> arguments = new ArrayList<>(rule.arguments().size());
                for (Sdd.Attr argument : rule.arguments()) {
                    arguments.add(instance(at(node, argument), argument.name(), ids, owner));
                }
                if (definitions.put(target, new Binding(rule, arguments)) != null) {
                    throw new IllegalStateException("Attribute " + target + " is defined more than once");
                }
                dependencies.put(target, List.copyOf(arguments));
            }
            List<ParseNode> children = node.children();
            for (int i = children.size() - 1; i >= 0; i--) {
                pending.push(children.get(i));
            }
        }
        for (List<Instance> arguments : dependencies.values()) {
            for (Instance argument : arguments) {
                if (!dependencies.containsKey(argument)) {
                    throw new IllegalStateException("Attribute " + argument + " is used but never defined");
                }
            }
        }
    }

    private static ParseNode at(ParseNode node, Sdd.Attr attr) {
        return attr.position() == 0 ? node : node.children().get(attr.position() - 1);
    }

    private static Instance instance(ParseNode node, String attribute, Map<ParseNode, Integer> ids,
                                     Map<Instance, ParseNode> owner) {
        Instance instance = new Instance(ids.get(node), node.symbol(), attribute);
        owner.put(instance, node);
        return instance;
    }

    private static List<Instance> topologicalOrder(Map<Instance, List<Instance>> dependencies) {
        Map<Instance, Integer> inDegree = new HashMap<>();
        Map<Instance, List<Instance>> successors = new HashMap<>();
        for (Map.Entry<Instance, List<Instance>> entry : dependencies.entrySet()) {
            inDegree.merge(entry.getKey(), entry.getValue().size(), Integer::sum);
            for (Instance argument : entry.getValue()) {
                successors.computeIfAbsent(argument, k -> new ArrayList<>()).add(entry.getKey());
            }
        }

        Deque<Instance> ready = new ArrayDeque<>();
        dependencies.keySet().forEach(v -> {
            if (inDegree.get(v) == 0) {
                ready.add(v);
            }
        });

        List<Instance> order = new ArrayList<>(dependencies.size());
        while (!ready.isEmpty()) {
            Instance vertex = ready.poll();
            order.add(vertex);
            for (Instance next : successors.getOrDefault(vertex, List.of())) {
                if (inDegree.merge(next, -1, Integer::sum) == 0) {
                    ready.add(next);
                }
            }
        }
        if (order.size() < dependencies.size()) {
            throw new CircularSddException(dependencies.size() - order.size());
        }
        return order;
    }
}
