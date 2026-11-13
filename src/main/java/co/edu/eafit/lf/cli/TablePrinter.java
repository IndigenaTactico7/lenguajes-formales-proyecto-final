package co.edu.eafit.lf.cli;

import co.edu.eafit.lf.analysis.FirstFollow;
import co.edu.eafit.lf.grammar.Grammar;
import co.edu.eafit.lf.grammar.Production;
import co.edu.eafit.lf.parsing.LL1Parser;
import co.edu.eafit.lf.parsing.Parser;
import co.edu.eafit.lf.parsing.SLR1Parser;
import co.edu.eafit.lf.parsing.Trace;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TablePrinter {

    private final PrintStream out;

    TablePrinter(PrintStream out) {
        this.out = out;
    }

    void print(Grammar grammar, FirstFollow sets, LL1Parser ll1, SLR1Parser slr1, List<String> strings) {
        section("FIRST and FOLLOW");
        out.println(sets.formatSets());

        List<Character> columns = new ArrayList<>(grammar.terminals());
        columns.add(Grammar.END);

        section("LL(1) table (grammar is " + (ll1.isLL1() ? "" : "not ") + "LL(1))");
        List<String> llHeader = new ArrayList<>(List.of(""));
        columns.forEach(c -> llHeader.add(String.valueOf(c)));
        List<List<String>> llRows = new ArrayList<>();
        for (char a : grammar.nonterminals()) {
            List<String> row = new ArrayList<>(List.of(String.valueOf(a)));
            Map<Character, Production> entries = ll1.table().getOrDefault(a, Map.of());
            columns.forEach(c -> row.add(entries.containsKey(c) ? entries.get(c).toString() : ""));
            llRows.add(row);
        }
        table(llHeader, llRows);

        section("LR(0) automaton");
        for (int i = 0; i < slr1.states().size(); i++) {
            out.println("I" + i + ":");
            slr1.states().get(i).forEach(item -> out.println("    " + item));
            slr1.transitionsOf(i).forEach((symbol, target) -> out.println("    goto(" + symbol + ") = I" + target));
        }

        section("SLR(1) table (grammar is " + (slr1.isSLR1() ? "" : "not ") + "SLR(1))");
        List<String> slrHeader = new ArrayList<>(List.of("state"));
        columns.forEach(c -> slrHeader.add(String.valueOf(c)));
        grammar.nonterminals().forEach(a -> slrHeader.add(String.valueOf(a)));
        List<List<String>> slrRows = new ArrayList<>();
        for (int i = 0; i < slr1.states().size(); i++) {
            List<String> row = new ArrayList<>(List.of(String.valueOf(i)));
            Map<Character, SLR1Parser.Action> action = slr1.actionTable().get(i);
            Map<Character, Integer> gotoRow = slr1.gotoTable().get(i);
            columns.forEach(c -> row.add(action.containsKey(c) ? action.get(c).toString() : ""));
            grammar.nonterminals().forEach(a -> row.add(gotoRow.containsKey(a) ? String.valueOf(gotoRow.get(a)) : ""));
            slrRows.add(row);
        }
        table(slrHeader, slrRows);

        for (String input : strings) {
            if (ll1.isLL1()) {
                printTrace("LL(1) trace of " + input, ll1, input);
            }
            if (slr1.isSLR1()) {
                printTrace("SLR(1) trace of " + input, slr1, input);
            }
        }
    }

    private void printTrace(String title, Parser parser, String input) {
        section(title);
        Trace trace = parser.trace(input);
        table(trace.header(), trace.rows());
        out.println(trace.accepted() ? "yes" : "no");
    }

    private void section(String title) {
        out.println();
        out.println("== " + title + " ==");
    }

    private void table(List<String> header, List<List<String>> rows) {
        int[] widths = new int[header.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = header.get(i).length();
            for (List<String> row : rows) {
                widths[i] = Math.max(widths[i], row.get(i).length());
            }
        }
        printRow(header, widths);
        StringBuilder rule = new StringBuilder();
        for (int width : widths) {
            rule.append("-".repeat(width)).append("-+-");
        }
        out.println(rule.substring(0, rule.length() - 3));
        rows.forEach(row -> printRow(row, widths));
    }

    private void printRow(List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            line.append(String.format("%-" + widths[i] + "s", cells.get(i)));
            if (i < cells.size() - 1) {
                line.append(" | ");
            }
        }
        out.println(line.toString().stripTrailing());
    }
}
