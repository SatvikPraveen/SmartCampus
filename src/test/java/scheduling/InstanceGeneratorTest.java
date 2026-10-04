package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import scheduling.experiment.InstanceGenerator;
import scheduling.model.Event;
import scheduling.model.TimetablingProblem;

class InstanceGeneratorTest {

    @Test
    void sameSeedSameInstance() {
        TimetablingProblem a = InstanceGenerator.generate(InstanceGenerator.SMALL, 9);
        TimetablingProblem b = InstanceGenerator.generate(InstanceGenerator.SMALL, 9);
        assertEquals(a.events(), b.events());
        assertEquals(a.rooms(), b.rooms());
        assertNotEquals(a.events(), InstanceGenerator.generate(InstanceGenerator.SMALL, 10).events());
    }

    @Test
    void respectsStructuralParameters() {
        var params = InstanceGenerator.SMALL;
        TimetablingProblem p = InstanceGenerator.generate(params, 1);
        assertEquals(params.events(), p.eventCount());
        assertEquals(params.rooms(), p.roomCount());
        assertEquals(params.days() * params.periodsPerDay(), p.slotCount());
        int enrolments = p.events().stream().mapToInt(Event::size).sum();
        assertEquals(params.students() * params.coursesPerStudent(), enrolments);
        int maxSize = p.events().stream().mapToInt(Event::size).max().orElseThrow();
        assertTrue(p.rooms().stream().anyMatch(r -> r.capacity() >= maxSize), "largest event must fit");
    }

    @Test
    void zipfSkewProducesLargerSpreadThanUniform() {
        var uniform = new InstanceGenerator.Params("u", 100, 2000, 4, 40, 5, 5, 9, 0.0, 1.1);
        var skewed = new InstanceGenerator.Params("z", 100, 2000, 4, 40, 5, 5, 9, 1.2, 1.1);
        assertTrue(maxSize(skewed) > 2 * maxSize(uniform));
    }

    private static int maxSize(InstanceGenerator.Params params) {
        return InstanceGenerator.generate(params, 3).events().stream().mapToInt(Event::size).max().orElseThrow();
    }
}
