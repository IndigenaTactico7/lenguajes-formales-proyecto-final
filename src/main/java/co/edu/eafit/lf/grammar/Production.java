package co.edu.eafit.lf.grammar;

import java.util.List;

public record Production(char head, List<Character> body) {

    public Production {
        body = List.copyOf(body);
    }

    public boolean isEpsilon() {
        return body.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append(head).append(" -> ");
        if (isEpsilon()) {
            return sb.append(Grammar.EPSILON).toString();
        }
        body.forEach(sb::append);
        return sb.toString();
    }
}
