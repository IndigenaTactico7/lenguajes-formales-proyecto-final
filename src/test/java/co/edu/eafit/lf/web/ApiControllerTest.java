package co.edu.eafit.lf.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.eafit.lf.web.ApiModels.AnalyzeResponse;
import co.edu.eafit.lf.web.ApiModels.GrammarRequest;
import co.edu.eafit.lf.web.ApiModels.ParseRequest;
import co.edu.eafit.lf.web.ApiModels.SddRequest;
import co.edu.eafit.lf.web.ApiModels.SddResponse;
import co.edu.eafit.lf.web.ApiModels.TraceResponse;
import org.junit.jupiter.api.Test;

class ApiControllerTest {

    private static final String EXAMPLE_1 = "3\nS -> S+T T\nT -> T*F F\nF -> (S) i\n";

    private final ApiController controller = new ApiController();

    @Test
    void analyzeReportsParsersAndConflicts() {
        AnalyzeResponse response = controller.analyze(new GrammarRequest(EXAMPLE_1));
        assertFalse(response.ll1());
        assertTrue(response.slr1());
        assertFalse(response.llConflicts().isEmpty());
        assertTrue(response.slrConflicts().isEmpty());
        assertEquals("{), +, $}", response.follow().get("S"));
    }

    @Test
    void parseReturnsTraceAndRejectsWrongParser() {
        TraceResponse response = controller.parse(new ParseRequest(EXAMPLE_1, "SLR1", "i+i"));
        assertTrue(response.accepted());
        assertEquals("accept", response.rows().getLast().getLast());
        assertThrows(IllegalStateException.class, () -> controller.parse(new ParseRequest(EXAMPLE_1, "LL1", "i")));
    }

    @Test
    void sddEvaluatesAndDetectsCircularity() {
        SddResponse response = controller.sdd(new SddRequest("8#17", false));
        assertEquals("15", response.value()); 
        assertEquals(16, response.order().size());
        assertTrue(controller.sdd(new SddRequest("8#17", true)).circular());
        assertFalse(controller.sdd(new SddRequest("8#", false)).accepted());
    }
}
