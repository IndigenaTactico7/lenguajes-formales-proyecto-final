package co.edu.eafit.lf.parsing;

import java.util.Optional;

public interface Parser {

    Optional<ParseNode> parse(String input);

    Trace trace(String input);

    default boolean accepts(String input) {
        return parse(input).isPresent();
    }
}
