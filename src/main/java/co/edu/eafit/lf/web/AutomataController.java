package co.edu.eafit.lf.web;

import co.edu.eafit.lf.automata.Dfa;
import co.edu.eafit.lf.automata.DfaMinimizer;
import co.edu.eafit.lf.automata.DfaSimulator;
import co.edu.eafit.lf.automata.DfaTextFormat;
import co.edu.eafit.lf.automata.Nfa;
import co.edu.eafit.lf.automata.SubsetConstruction;
import co.edu.eafit.lf.web.AutomataModels.DfaResponse;
import co.edu.eafit.lf.web.AutomataModels.DfaState;
import co.edu.eafit.lf.web.AutomataModels.DfaTransition;
import co.edu.eafit.lf.web.AutomataModels.MarkingTable;
import co.edu.eafit.lf.web.AutomataModels.MinimizeResponse;
import co.edu.eafit.lf.web.AutomataModels.NfaRequest;
import co.edu.eafit.lf.web.AutomataModels.Pair;
import co.edu.eafit.lf.web.AutomataModels.SimulateRequest;
import co.edu.eafit.lf.web.AutomataModels.SimulationResponse;
import co.edu.eafit.lf.web.AutomataModels.Transition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/automata", ""})
public class AutomataController {

    @PostMapping("/convert")
    public DfaResponse convert(@RequestBody NfaRequest request) {
        return toResponse(SubsetConstruction.convert(toNfa(request)));
    }

    @PostMapping("/simulate")
    public SimulationResponse simulate(@RequestBody SimulateRequest request) {
        if (request.dfa() == null) {
            throw new IllegalArgumentException("The request has no DFA.");
        }
        DfaSimulator.Result result = DfaSimulator.simulate(toDfa(request.dfa()), request.input());
        return new SimulationResponse(result.path(), result.accepted(), result.message());
    }

    @PostMapping("/minimize")
    public MinimizeResponse minimize(@RequestBody DfaResponse request) {
        Dfa dfa = toDfa(request);
        DfaMinimizer.MarkingTable table = DfaMinimizer.markingTable(dfa);
        List<DfaMinimizer.Pair> pairs = DfaMinimizer.equivalentPairs(table);

        List<List<Integer>> rounds = new ArrayList<>();
        for (int[] row : table.round()) {
            List<Integer> values = new ArrayList<>(row.length);
            for (int value : row) {
                values.add(value);
            }
            rounds.add(values);
        }
        return new MinimizeResponse(
                pairs.stream().map(p -> new Pair(p.first(), p.second())).toList(),
                toResponse(DfaMinimizer.minimize(dfa, pairs)),
                new MarkingTable(table.states(), rounds));
    }

    @PostMapping(value = "/minimize/batch", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String minimizeBatch(@RequestBody String body) {
        StringJoiner output = new StringJoiner("\n", "", "\n");
        for (Dfa dfa : DfaTextFormat.parseCases(body)) {
            output.add(DfaTextFormat.formatPairs(DfaMinimizer.equivalentPairs(dfa)));
        }
        return output.toString();
    }

    private static Nfa toNfa(NfaRequest request) {
        if (request.initial() == null) {
            throw new IllegalArgumentException("The NFA has no initial state.");
        }
        Map<Integer, Map<String, Set<Integer>>> delta = new LinkedHashMap<>();
        for (Transition t : request.transitions()) {
            if (t.from() == null || t.to() == null) {
                throw new IllegalArgumentException("Every transition needs a source and a target state.");
            }
            delta.computeIfAbsent(t.from(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(t.symbol(), k -> new LinkedHashSet<>())
                    .add(t.to());
        }
        return new Nfa(new LinkedHashSet<>(request.states()), request.alphabet(), request.initial(),
                new HashSet<>(request.accepting()), delta);
    }

    private static Dfa toDfa(DfaResponse dto) {
        Set<String> accepting = new HashSet<>(dto.acceptingStates());
        List<Dfa.State> states = new ArrayList<>();
        for (String name : dto.dfaStates()) {
            states.add(new Dfa.State(name, List.of(), accepting.contains(name)));
        }
        Map<String, Map<String, String>> transitions = new LinkedHashMap<>();
        for (DfaTransition t : dto.transitions()) {
            transitions.computeIfAbsent(t.from(), k -> new LinkedHashMap<>()).put(t.symbol(), t.to());
        }
        return new Dfa(dto.initialState(), states, dto.alphabet(), transitions, List.of());
    }

    private static DfaResponse toResponse(Dfa dfa) {
        List<DfaTransition> transitions = new ArrayList<>();
        dfa.transitions().forEach((from, row) -> row.forEach((symbol, to) ->
                transitions.add(new DfaTransition(from, symbol, to))));

        List<String> names = new ArrayList<>();
        List<String> accepting = new ArrayList<>();
        List<DfaState> details = new ArrayList<>();
        for (Dfa.State state : dfa.states()) {
            names.add(state.name());
            if (state.accepting()) {
                accepting.add(state.name());
            }
            details.add(new DfaState(state.name(), state.subset(), state.accepting()));
        }
        return new DfaResponse(names, dfa.initialState(), dfa.alphabet(), transitions, accepting,
                dfa.unreachableNfaStates(), details);
    }
}
