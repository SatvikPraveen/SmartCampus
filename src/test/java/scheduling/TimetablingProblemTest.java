package scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import scheduling.model.Event;
import scheduling.model.Precedence;
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

    @Test
    void withoutSideConstraintsEverythingIsAllowed() {
        TimetablingProblem p = Fixtures.tiny();
        for (int e = 0; e < p.eventCount(); e++) {
            assertArrayEquals(IntStream.range(0, p.slotCount()).toArray(), p.availableSlotsOf(e));
            assertArrayEquals(new int[] {0, 1}, p.featureRoomsOf(e));
            assertTrue(p.hasRequiredFeatures(e, 0));
            assertTrue(p.isAvailable(e, p.slotCount() - 1));
            assertArrayEquals(new int[] {}, p.precedencesOf(e));
        }
        assertEquals(0, p.precedenceCount());
        assertEquals(Set.of(), new Room("r", 1).features());
        assertEquals(Set.of(), new Event("e", null, Set.of()).requiredFeatures());
    }

    @Test
    void roomFeaturesRestrictSuitableRooms() {
        TimetablingProblem p = new TimetablingProblem(
                List.of(new Event("lab", null, Set.of("a"), Set.of("bench", "sink")),
                        new Event("talk", null, Set.of("a", "b", "c"))),
                List.of(new Room("big", 10), new Room("wet", 2, Set.of("bench", "sink", "hood")),
                        new Room("dry", 3, Set.of("bench"))),
                1, 2);
        assertArrayEquals(new int[] {1}, p.suitableRoomsOf(0));
        assertArrayEquals(new int[] {1}, p.featureRoomsOf(0));
        assertFalse(p.hasRequiredFeatures(0, 2));
        // talk needs 3 seats: wet is too small, dry and big fit (best fit first).
        assertArrayEquals(new int[] {2, 0}, p.suitableRoomsOf(1));
    }

    @Test
    void availabilityAndPrecedenceAreIndexed() {
        List<Event> events = List.of(new Event("x", null, Set.of("s")), new Event("y", null, Set.of("s")),
                new Event("z", null, Set.of("t")));
        TimetablingProblem p = new TimetablingProblem(events, List.of(new Room("r", 1)), 2, 2,
                Map.of("y", Set.of(3, 1)),
                List.of(new Precedence("x", "y"), new Precedence("y", "z"), new Precedence("x", "y")));
        assertArrayEquals(new int[] {1, 3}, p.availableSlotsOf(1));
        assertTrue(p.isAvailable(1, 3));
        assertFalse(p.isAvailable(1, 0));
        assertEquals(4, p.availableSlotsOf(0).length);
        assertEquals(2, p.precedenceCount(), "duplicates are ignored");
        assertEquals(0, p.precedenceBefore(0));
        assertEquals(1, p.precedenceAfter(0));
        assertArrayEquals(new int[] {0, 1}, p.precedencesOf(1));
        assertArrayEquals(new int[] {1}, p.precedencesOf(2));
    }

    @Test
    void rejectsInvalidSideConstraints() {
        List<Event> events = List.of(new Event("x", null, Set.of("s")));
        List<Room> rooms = List.of(new Room("r", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new TimetablingProblem(events, rooms, 1, 2, Map.of("x", Set.of(2)), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new TimetablingProblem(events, rooms, 1, 2, Map.of("nope", Set.of(0)), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new TimetablingProblem(events, rooms, 1, 2, Map.of(), List.of(new Precedence("x", "nope"))));
        assertThrows(IllegalArgumentException.class, () -> new Precedence("x", "x"));
        assertThrows(IllegalArgumentException.class, () -> new Precedence(null, "x"));
    }
}
