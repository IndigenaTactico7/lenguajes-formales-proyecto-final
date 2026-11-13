package co.edu.eafit.lf.web;

import co.edu.eafit.lf.analysis.FirstFollow;
import co.edu.eafit.lf.grammar.Grammar;
import co.edu.eafit.lf.grammar.GrammarReader;
import co.edu.eafit.lf.parsing.LL1Parser;
import co.edu.eafit.lf.parsing.ParseNode;
import co.edu.eafit.lf.parsing.Parser;
import co.edu.eafit.lf.parsing.SLR1Parser;
import co.edu.eafit.lf.parsing.Trace;
import co.edu.eafit.lf.sdd.BaseNumberSdd;
import co.edu.eafit.lf.sdd.SddEvaluator;
import co.edu.eafit.lf.web.ApiModels.AnalyzeResponse;
import co.edu.eafit.lf.web.ApiModels.Edge;
import co.edu.eafit.lf.web.ApiModels.GrammarRequest;
import co.edu.eafit.lf.web.ApiModels.ParseRequest;
import co.edu.eafit.lf.web.ApiModels.SddRequest;
import co.edu.eafit.lf.web.ApiModels.SddResponse;
import co.edu.eafit.lf.web.ApiModels.State;
import co.edu.eafit.lf.web.ApiModels.Step;
import co.edu.eafit.lf.web.ApiModels.TraceResponse;
import co.edu.eafit.lf.web.ApiModels.TreeNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final SddEvaluator sddEvaluator = new SddEvaluator(BaseNumberSdd.create());
    private final SddEvaluator circularEvaluator = new SddEvaluator(BaseNumberSdd.createCircular());

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody GrammarRequest request) {
        Grammar grammar = readGrammar(request.grammar());
        FirstFollow sets = new FirstFollow(grammar);
        LL1Parser ll1 = new LL1Parser(grammar, sets);
        SLR1Parser slr1 = new SLR1Parser(grammar, sets);

        Map<String, String> first = new LinkedHashMap<>();
        Map<String, String> follow = new LinkedHashMap<>();
        for (char a : grammar.nonterminals()) {
            first.put(String.valueOf(a), FirstFollow.format(sets.first(a)));
            follow.put(String.valueOf(a), FirstFollow.format(sets.follow(a)));
        }

        Map<String, Map<String, String>> llTable = new LinkedHashMap<>();
        ll1.table().forEach((head, row) -> llTable.put(String.valueOf(head), stringKeys(row, Object::toString)));

        List<State> states = new ArrayList<>();
        for (int i = 0; i < slr1.states().size(); i++) {
            List<String> items = slr1.states().get(i).stream().map(Object::toString).toList();
            states.add(new State(i, items, stringKeys(slr1.transitionsOf(i), Integer::intValue)));
        }

        return new AnalyzeResponse(
                symbols(grammar.nonterminals()), symbols(grammar.terminals()), first, follow,
                ll1.isLL1(), slr1.isSLR1(),
                llTable, ll1.conflicts().stream().map(c -> c.nonterminal() + "," + c.terminal()).toList(),
                states,
                slr1.actionTable().stream().map(row -> stringKeys(row, Object::toString)).toList(),
                slr1.gotoTable().stream().map(row -> stringKeys(row, Integer::intValue)).toList(),
                slr1.conflicts().stream().map(c -> c.state() + "," + c.terminal()).toList());
    }

    @PostMapping("/parse")
    public TraceResponse parse(@RequestBody ParseRequest request) {
        Grammar grammar = readGrammar(request.grammar());
        FirstFollow sets = new FirstFollow(grammar);
        Parser parser = "SLR1".equals(request.parser()) ? new SLR1Parser(grammar, sets) : new LL1Parser(grammar, sets);
        String input = request.input() == null || request.input().isBlank()
                ? String.valueOf(Grammar.EPSILON) : request.input().strip();
        Trace trace = parser.trace(input);
        TreeNode tree = trace.tree().map(root -> toTree(root, preorderIds(root)::get)).orElse(null);
        return new TraceResponse(trace.accepted(), trace.header(), trace.rows(), tree);
    }

    @PostMapping("/sdd")
    public SddResponse sdd(@RequestBody SddRequest request) {
        SddEvaluator evaluator = request.circular() ? circularEvaluator : sddEvaluator;
        Optional<SddEvaluator.Evaluation> result;
        try {
            result = evaluator.evaluate(request.input() == null ? "" : request.input().strip());
        } catch (SddEvaluator.CircularSddException e) {
            return new SddResponse(true, true, null, List.of(), List.of(), null);
        }
        if (result.isEmpty()) {
            return new SddResponse(false, false, null, List.of(), List.of(), null);
        }
        SddEvaluator.Evaluation evaluation = result.get();
        Map<Integer, ParseNode> nodes = new HashMap<>();
        evaluation.ids().forEach((node, id) -> nodes.put(id, node));

        List<Edge> edges = new ArrayList<>();
        evaluation.dependencies().forEach((target, arguments) -> arguments.forEach(argument -> edges.add(
                new Edge(argument.nodeId(), argument.attribute(), target.nodeId(), target.attribute()))));
        List<Step> order = evaluation.order().stream()
                .map(i -> new Step(i.nodeId(), String.valueOf(i.symbol()), i.attribute(),
                        display(nodes.get(i.nodeId()).attribute(i.attribute()))))
                .toList();
        TreeNode tree = toTree(evaluation.tree(), evaluation.ids()::get);
        return new SddResponse(true, false, tree, edges, order, display(evaluation.mainValue()));
    }

    private static Grammar readGrammar(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("The grammar is empty");
        }
        try {
            return GrammarReader.read(new BufferedReader(new StringReader(text)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> symbols(Set<Character> symbols) {
        return symbols.stream().map(String::valueOf).toList();
    }

    private static <V, R> Map<String, R> stringKeys(Map<Character, V> map, Function<V, R> value) {
        Map<String, R> result = new TreeMap<>();
        map.forEach((key, v) -> result.put(String.valueOf(key), value.apply(v)));
        return result;
    }

    private static Map<ParseNode, Integer> preorderIds(ParseNode root) {
        Map<ParseNode, Integer> ids = new IdentityHashMap<>();
        assignIds(root, ids);
        return ids;
    }

    private static void assignIds(ParseNode node, Map<ParseNode, Integer> ids) {
        ids.put(node, ids.size() + 1);
        node.children().forEach(child -> assignIds(child, ids));
    }

    private static Object display(Object value) {
        return value instanceof java.math.BigInteger ? value.toString() : value;
    }

    private static Map<String, Object> displayAll(Map<String, Object> attributes) {
        Map<String, Object> result = new TreeMap<>();
        attributes.forEach((name, value) -> result.put(name, display(value)));
        return result;
    }

    private static TreeNode toTree(ParseNode node, ToIntFunction<ParseNode> id) {
        List<TreeNode> children = node.children().stream().map(child -> toTree(child, id)).toList();
        return new TreeNode(id.applyAsInt(node), String.valueOf(node.symbol()),
                displayAll(node.attributes()), children);
    }
}
