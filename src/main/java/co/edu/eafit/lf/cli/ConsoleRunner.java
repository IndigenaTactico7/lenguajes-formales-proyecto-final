package co.edu.eafit.lf.cli;

import co.edu.eafit.lf.analysis.FirstFollow;
import co.edu.eafit.lf.grammar.Grammar;
import co.edu.eafit.lf.grammar.GrammarReader;
import co.edu.eafit.lf.parsing.LL1Parser;
import co.edu.eafit.lf.parsing.Parser;
import co.edu.eafit.lf.parsing.SLR1Parser;
import co.edu.eafit.lf.sdd.BaseNumberSdd;
import co.edu.eafit.lf.sdd.Sdd;
import co.edu.eafit.lf.sdd.SddEvaluator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnNotWebApplication
public class ConsoleRunner implements ApplicationRunner {

    private static final String SETS_OPTION = "sets";
    private static final String SDD_OPTION = "sdd";
    private static final String CIRCULAR_OPTION = "circular";
    private static final String TABLES_OPTION = "tables";
    private static final String MENU = "Select a parser (T: for LL(1), B: for SLR(1), Q: quit):";

    @Override
    public void run(ApplicationArguments args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        skipByteOrderMark(in);
        if (args.containsOption(SDD_OPTION)) {
            sddSession(in, System.out, args.containsOption(CIRCULAR_OPTION));
        } else if (args.containsOption(TABLES_OPTION)) {
            tablesSession(in, System.out);
        } else {
            session(in, System.out, args.containsOption(SETS_OPTION));
        }
        System.out.flush();
    }

    public void tablesSession(BufferedReader in, PrintStream out) throws IOException {
        Grammar grammar = GrammarReader.read(in);
        FirstFollow sets = new FirstFollow(grammar);
        List<String> strings = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null && !line.isBlank()) {
            strings.add(line.strip());
        }
        new TablePrinter(out).print(grammar, sets, new LL1Parser(grammar, sets), new SLR1Parser(grammar, sets), strings);
    }

    private static void skipByteOrderMark(BufferedReader in) throws IOException {
        in.mark(1);
        if (in.read() != '﻿') {
            in.reset();
        }
    }

    public void sddSession(BufferedReader in, PrintStream out, boolean circular) throws IOException {
        Sdd sdd = circular ? BaseNumberSdd.createCircular() : BaseNumberSdd.create();
        SddEvaluator evaluator = new SddEvaluator(sdd);
        String line;
        while ((line = in.readLine()) != null && !line.isBlank()) {
            Optional<SddEvaluator.Evaluation> evaluation;
            try {
                evaluation = evaluator.evaluate(line.strip());
            } catch (SddEvaluator.CircularSddException e) {
                out.println("The SDD is circular: the dependency graph has a cycle.");
                return;
            }
            if (evaluation.isEmpty()) {
                out.println("no");
                continue;
            }
            SddEvaluator.Evaluation result = evaluation.get();
            out.println("Annotated parse tree:");
            out.println(result.formatTree());
            out.println("Evaluation order:");
            out.println(result.formatOrder());
            out.println(Grammar.START + "." + sdd.mainAttribute() + " = " + result.mainValue());
        }
    }

    public void session(BufferedReader in, PrintStream out, boolean printSets) throws IOException {
        Grammar grammar = GrammarReader.read(in);
        FirstFollow sets = new FirstFollow(grammar);
        if (printSets) {
            out.println(sets.formatSets());
        }

        LL1Parser ll1 = new LL1Parser(grammar, sets);
        SLR1Parser slr1 = new SLR1Parser(grammar, sets);

        if (ll1.isLL1() && slr1.isSLR1()) {
            chooseParser(in, out, ll1, slr1);
        } else if (ll1.isLL1()) {
            out.println("Grammar is LL(1).");
            parseStrings(ll1, in, out);
        } else if (slr1.isSLR1()) {
            out.println("Grammar is SLR(1).");
            parseStrings(slr1, in, out);
        } else {
            out.println("Grammar is neither LL(1) nor SLR(1).");
        }
    }

    private static void chooseParser(BufferedReader in, PrintStream out, Parser ll1, Parser slr1) throws IOException {
        while (true) {
            out.println(MENU);
            String choice = in.readLine();
            if (choice == null) {
                return;
            }
            switch (choice.strip()) {
                case "T" -> parseStrings(ll1, in, out);
                case "B" -> parseStrings(slr1, in, out);
                case "Q" -> {
                    return;
                }
                default -> {

                }
            }
        }
    }

    private static void parseStrings(Parser parser, BufferedReader in, PrintStream out) throws IOException {
        String line;
        while ((line = in.readLine()) != null && !line.isBlank()) {
            out.println(parser.accepts(line.strip()) ? "yes" : "no");
        }
    }
}
