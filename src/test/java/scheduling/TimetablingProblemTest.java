package scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import scheduling.model.Event;
import scheduling.model.Room;
import scheduling.model.TimetablingProblem;

class TimetablingProblemTest {

    @Test
    void conflictGraphLinksSharedStudentsAndInstructors() {
        TimetablingProblem p = Fixtures.tiny();
        assertArrayEquals(new int[] {1, 2}, p.neighboursOf(0));
        assertArrayEquals(new int[] {0}, p.neighboursOf(1));
        assertArrayEquals(new int[] {0}, p.neighboursOf(2));
        assertArrayEquals(new int[] {}, p.neighboursOf(3));
        assertEquals(2, p.conflictEdgeCount());
        assertEquals(2.0 * 2 / (4 * 3), p.conflictDensity(), 1e-12);
    }

    @Test
    void suitableRoomsAreBestFitFirst() {
        TimetablingProblem p = Fixtures.tiny();
        assertArrayEquals(new int[] {0, 1}, p.suitableRoomsOf(0));
        assertArrayEquals(new int[] {1}, p.suitableRoomsOf(3));
    }

    @Test
    void slotArithmeticRoundTrips() {
        TimetablingProblem p = new TimetablingProblem(
                List.of(new Event("e", null, Set.of("s"))), List.of(new Room("r", 1)), 5, 9);
        for (int t = 0; t < p.slotCount(); t++) {
            assertEquals(t, p.slot(p.dayOf(t), p.periodOf(t)));
        }
    }

    @Test
    void rejectsInvalidInput() {
        List<Room> rooms = List.of(new Room("r", 1));
        assertThrows(IllegalArgumentException.class, () -> new TimetablingProblem(List.of(), rooms, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Room("r", 0));
        assertThrows(IllegalArgumentException.class, () -> new TimetablingProblem(
                List.of(new Event("e", null, Set.of()), new Event("e", null, Set.of())), rooms, 1, 1));
    }
}
