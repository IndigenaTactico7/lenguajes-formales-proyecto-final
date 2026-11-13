package co.edu.eafit.lf.sdd;

import co.edu.eafit.lf.grammar.Production;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record Sdd(String grammarText, Map<Production, List<Rule>> rules, String mainAttribute) {

    public record Attr(int position, String name) {
    }

    public record Rule(Attr target, List<Attr> arguments, Function<List<Object>, Object> function) {

        public Rule {
            arguments = List.copyOf(arguments);
        }
    }

    public Sdd {
        rules = Map.copyOf(rules);
    }

    public static Attr attr(int position, String name) {
        return new Attr(position, name);
    }

    public static Rule rule(Attr target, Function<List<Object>, Object> function, Attr... arguments) {
        return new Rule(target, List.of(arguments), function);
    }
}
