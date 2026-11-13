package co.edu.eafit.lf.sdd;

import static co.edu.eafit.lf.sdd.Sdd.attr;
import static co.edu.eafit.lf.sdd.Sdd.rule;

import co.edu.eafit.lf.grammar.Production;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class BaseNumberSdd {

    static final String GRAMMAR = """
            4
            S -> D#N
            N -> DR
            R -> DR e
            D -> 0 1 2 3 4 5 6 7 8 9
            """;

    private static final Function<List<Object>, Object> COPY = args -> args.get(0);

    private BaseNumberSdd() {
    }

    public static Sdd create() {
        return build(false);
    }

    public static Sdd createCircular() {
        return build(true);
    }

    private static Sdd build(boolean circular) {
        Map<Production, List<Sdd.Rule>> rules = new LinkedHashMap<>();

        rules.put(production('S', "D#N"), List.of(
                rule(attr(3, "base"), COPY, attr(1, "val")),
                rule(attr(0, "val"), COPY, attr(3, "val"))));

        rules.put(production('N', "DR"), List.of(
                rule(attr(2, "base"), COPY, attr(0, "base")),
                circular
                        ? rule(attr(2, "acc"), COPY, attr(0, "val"))
                        : rule(attr(2, "acc"), COPY, attr(1, "val")),
                rule(attr(0, "val"), COPY, attr(2, "val"))));

        rules.put(production('R', "DR"), List.of(
                rule(attr(2, "base"), COPY, attr(0, "base")),
                rule(attr(2, "acc"), args -> ((BigInteger) args.get(0)).multiply((BigInteger) args.get(1)).add((BigInteger) args.get(2)),
                        attr(0, "acc"), attr(0, "base"), attr(1, "val")),
                rule(attr(0, "val"), COPY, attr(2, "val"))));

        rules.put(production('R', ""), List.of(
                rule(attr(0, "val"), COPY, attr(0, "acc"))));

        for (char k = '0'; k <= '9'; k++) {
            rules.put(production('D', String.valueOf(k)), List.of(
                    rule(attr(0, "val"), args -> BigInteger.valueOf(Character.digit((char) args.get(0), 10)),
                            attr(1, SddEvaluator.LEXVAL))));
        }
        return new Sdd(GRAMMAR, rules, "val");
    }

    private static Production production(char head, String body) {
        List<Character> symbols = new ArrayList<>(body.length());
        for (char c : body.toCharArray()) {
            symbols.add(c);
        }
        return new Production(head, symbols);
    }
}
