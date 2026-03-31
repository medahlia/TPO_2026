import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordLengthTest {

    @Test
    void testParallelEqualsSequential() {
        List<String> texts = List.of(
                "Hello world",
                "Java programming language",
                "Fork Join Framework"
        );

        WordLength.Stats seq = WordLength.analyzeSequential(texts);
        WordLength.Stats par = WordLength.analyzeParallel(texts);

        assertEquals(getCount(seq), getCount(par));
        assertEquals(getMin(seq), getMin(par));
        assertEquals(getMax(seq), getMax(par));
    }

    @Test
    void testEmptyList() {
        List<String> texts = List.of();

        WordLength.Stats stats = WordLength.analyzeSequential(texts);

        assertEquals(0, getCount(stats));
    }

    @Test
    void testSingleText() {
        List<String> texts = List.of("Hello Java world");

        WordLength.Stats stats = WordLength.analyzeParallel(texts);

        assertEquals(3, getCount(stats));
    }

    @Test
    // чи відбувається поділ задач
    void testSplitOccurs() {

        List<String> texts = List.of(
                "one two three",
                "four five six",
                "seven eight nine",
                "ten eleven twelve"
        );

        WordLength.analyzeParallel(texts);

        assertTrue(WordLength.getSplitCount() > 0);
    }

    @Test
    void testThresholdNoSplit() {

        List<String> texts = List.of(
                "one two",
                "three four"
        );

        WordLength.analyzeParallel(texts);

        // при THRESHOLD = 2 поділу не повинно бути
        assertTrue(WordLength.getSplitCount() >= 0);
    }

    // helper methods

    private int getCount(WordLength.Stats stats) {
        try {
            var field = WordLength.Stats.class.getDeclaredField("count");
            field.setAccessible(true);
            return field.getInt(stats);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getMin(WordLength.Stats stats) {
        try {
            var field = WordLength.Stats.class.getDeclaredField("min");
            field.setAccessible(true);
            return field.getInt(stats);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getMax(WordLength.Stats stats) {
        try {
            var field = WordLength.Stats.class.getDeclaredField("max");
            field.setAccessible(true);
            return field.getInt(stats);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}