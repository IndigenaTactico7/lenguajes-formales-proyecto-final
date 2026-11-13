package co.edu.eafit.lf.sdd;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SddEvaluatorTest {

    private static final SddEvaluator EVALUATOR = new SddEvaluator(BaseNumberSdd.create());

    private static Object value(String input) {
        return EVALUATOR.evaluate(input).orElseThrow().mainValue();
    }

    @Test
    void shortestString() {
        Optional<SddEvaluator.Evaluation> eval = EVALUATOR.evaluate("2#0");
        assertTrue(eval.isPresent());
        assertEquals(BigInteger.ZERO, eval.get().mainValue());
    }

    @Test
    void octalNumber() {
        assertEquals(BigInteger.valueOf(15), value("8#17"));
    }

    @Test
    void binaryNumber() {
        assertEquals(BigInteger.valueOf(11), value("2#1011"));
    }

    @Test
    void deepRecursion() {
        assertEquals(BigInteger.valueOf(255), value("2#11111111"));
    }

    @Test
    void longNumbersDoNotOverflow() {

        BigInteger expected = BigInteger.ZERO;
        for (int i = 0; i < 40; i++) {
            expected = expected.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(9));
        }
        assertEquals(expected, value("9#" + "9".repeat(40)));
        assertTrue(expected.signum() > 0);
    }

    @Test
    void veryDeepTreeCanBePrinted() {
        SddEvaluator.Evaluation evaluation = EVALUATOR.evaluate("9#" + "1".repeat(5000)).orElseThrow();
        assertTrue(evaluation.formatTree().lines().count() > 10000);
    }

    @Test
    void stringsOutsideLanguageAreRejected() {
        assertTrue(EVALUATOR.evaluate("9#").isEmpty());
        assertTrue(EVALUATOR.evaluate("#12").isEmpty());
        assertTrue(EVALUATOR.evaluate("12").isEmpty());
    }

    @Test
    void evaluationOrderRespectsDependencies() {
        SddEvaluator.Evaluation evaluation = EVALUATOR.evaluate("2#1011").orElseThrow();
        Set<SddEvaluator.Instance> seen = new HashSet<>();
        for (SddEvaluator.Instance instance : evaluation.order()) {
            for (SddEvaluator.Instance dep : evaluation.dependencies().getOrDefault(instance, List.of())) {
                assertTrue(seen.contains(dep));
            }
            seen.add(instance);
        }
    }

    @Test
    void circularSddIsDetected() {
        assertThrows(SddEvaluator.CircularSddException.class, () -> new SddEvaluator(BaseNumberSdd.createCircular()).evaluate("8#17"));
    }
}
