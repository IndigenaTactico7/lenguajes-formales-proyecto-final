package co.edu.eafit.lf.web;

import java.util.List;
import java.util.Map;

public final class ApiModels {

    private ApiModels() {
    }

    public record GrammarRequest(String grammar) {
    }

    public record ParseRequest(String grammar, String parser, String input) {
    }

    public record SddRequest(String input, boolean circular) {
    }

    public record ErrorResponse(String error) {
    }

    public record AnalyzeResponse(List<String> nonterminals, List<String> terminals,
                                  Map<String, String> first, Map<String, String> follow,
                                  boolean ll1, boolean slr1,
                                  Map<String, Map<String, String>> llTable, List<String> llConflicts,
                                  List<State> states, List<Map<String, String>> action,
                                  List<Map<String, Integer>> gotoTable, List<String> slrConflicts) {
    }

    public record State(int id, List<String> items, Map<String, Integer> transitions) {
    }

    public record TraceResponse(boolean accepted, List<String> header, List<List<String>> rows, TreeNode tree) {
    }

    public record TreeNode(int id, String symbol, Map<String, Object> attributes, List<TreeNode> children) {
    }

    public record SddResponse(boolean accepted, boolean circular, TreeNode tree, List<Edge> edges,
                              List<Step> order, Object value) {
    }

    public record Edge(int fromNode, String fromAttribute, int toNode, String toAttribute) {
    }

    public record Step(int node, String symbol, String attribute, Object value) {
    }
}
