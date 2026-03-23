package cn.edu.sdu.java.server.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class ScoreMarkValidatorTest {
    @Test
    void rejectsNull() {
        assertNull(ScoreMarkValidator.parseOrNull(null));
    }

    @Test
    void rejectsEmpty() {
        assertNull(ScoreMarkValidator.parseOrNull(""));
        assertNull(ScoreMarkValidator.parseOrNull("   "));
    }

    @Test
    void rejectsNegative() {
        assertNull(ScoreMarkValidator.parseOrNull("-1"));
        assertNull(ScoreMarkValidator.parseOrNull("-0.1"));
    }

    @Test
    void acceptsZero() {
        BigDecimal v = ScoreMarkValidator.parseOrNull("0");
        assertNotNull(v);
        assertEquals(new BigDecimal("0.0"), v);
    }

    @Test
    void acceptsHundred() {
        BigDecimal v1 = ScoreMarkValidator.parseOrNull("100");
        BigDecimal v2 = ScoreMarkValidator.parseOrNull("100.0");
        assertEquals(new BigDecimal("100.0"), v1);
        assertEquals(new BigDecimal("100.0"), v2);
    }

    @Test
    void rejectsOverHundred() {
        assertNull(ScoreMarkValidator.parseOrNull("100.1"));
        assertNull(ScoreMarkValidator.parseOrNull("101"));
    }

    @Test
    void acceptsOneDecimal() {
        BigDecimal v = ScoreMarkValidator.parseOrNull("99.9");
        assertNotNull(v);
        assertEquals(new BigDecimal("99.9"), v);
    }

    @Test
    void rejectsMoreThanOneDecimal() {
        assertNull(ScoreMarkValidator.parseOrNull("99.99"));
        assertNull(ScoreMarkValidator.parseOrNull("0.00"));
    }

    @Test
    void rejectsNonNumeric() {
        assertNull(ScoreMarkValidator.parseOrNull("abc"));
        assertNull(ScoreMarkValidator.parseOrNull("1a"));
        assertNull(ScoreMarkValidator.parseOrNull(".."));
    }

    @Test
    void concurrentValidationIsStable() throws ExecutionException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<BigDecimal>> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> ScoreMarkValidator.parseOrNull("88.5"));
            tasks.add(() -> ScoreMarkValidator.parseOrNull("101"));
            tasks.add(() -> ScoreMarkValidator.parseOrNull("0"));
            tasks.add(() -> ScoreMarkValidator.parseOrNull("99.9"));
        }
        List<Future<BigDecimal>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        for (Future<BigDecimal> f : futures) {
            BigDecimal v = f.get();
            if (v != null) {
                assertTrue(v.compareTo(BigDecimal.ZERO) >= 0);
                assertTrue(v.compareTo(new BigDecimal("100.0")) <= 0);
                assertTrue(v.scale() <= 1);
            }
        }
    }
}

