package co.edu.eafit.lf.parsing;

import java.util.List;
import java.util.Optional;

public record Trace(List<String> header, List<List<String>> rows, Optional<ParseNode> tree) {

    public Trace {
        header = List.copyOf(header);
        rows = List.copyOf(rows);
    }

    public boolean accepted() {
        return tree.isPresent();
    }
}
