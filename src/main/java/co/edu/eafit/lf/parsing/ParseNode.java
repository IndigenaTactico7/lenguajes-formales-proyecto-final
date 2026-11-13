package co.edu.eafit.lf.parsing;

import co.edu.eafit.lf.grammar.Production;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParseNode {

    private final char symbol;
    private Production production;
    private final List<ParseNode> children = new ArrayList<>();
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public ParseNode(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    public Production production() {
        return production;
    }

    public List<ParseNode> children() {
        return Collections.unmodifiableList(children);
    }

    public boolean isLeaf() {
        return production == null;
    }

    public Map<String, Object> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Object attribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    void expand(Production production, List<ParseNode> children) {
        this.production = production;
        this.children.clear();
        this.children.addAll(children);
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return String.valueOf(symbol);
        }
        StringBuilder sb = new StringBuilder().append(symbol).append('[');
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(children.get(i));
        }
        return sb.append(']').toString();
    }
}
