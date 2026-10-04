package scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import scheduling.eval.CostModel;
import scheduling.io.Itc2007Loader;
import scheduling.model.TimetablingProblem;

/**
 * Tests the {@code .tim} reader against a hand-written fixture
 * ({@code src/test/resources/scheduling/itc2007-tiny.tim}): 4 events, 2 rooms, 2 features,
 * 3 students. The fixture puts each matrix row on one line for readability; the format is
 * whitespace-separated, so this is equivalent to the competition's one-value-per-line files.
 */
class Itc2007LoaderTest {

    static Path fixture() throws URISyntaxException {
        return Path.of(Itc2007LoaderTest.class.getResource("/scheduling/itc2007-tiny.tim").toURI());
    }

    @Test
    void parsesEverySection() throws Exception {
        TimetablingProblem p = Itc2007Loader.load(fixture());
        assertEquals(4, p.eventCount());
        assertEquals(2, p.roomCount());
        assertEquals(3, p.studentCount());
        assertEquals(45, p.slotCount());
        assertEquals(5, p.days());

        // Rooms: sizes 2 and 1; room 0 offers feature 0, room 1 offers feature 1.
        assertEquals(2, p.capacityOf(0));
        assertEquals(1, p.capacityOf(1));
        assertEquals(Set.of("f0"), p.rooms().get(0).features());
        assertEquals(Set.of("f1"), p.rooms().get(1).features());

        // Attendance: e0 {s0, s1}, e1 {s0}, e2 {s1, s2}, e3 {}.
        assertEquals(Set.of("s0", "s1"), p.events().get(0).studentIds());
        assertEquals(Set.of("s0"), p.events().get(1).studentIds());
        assertEquals(Set.of("s1", "s2"), p.events().get(2).studentIds());
        assertEquals(Set.of(), p.events().get(3).studentIds());
        assertArrayEquals(new int[] {1, 2}, p.neighboursOf(0));
        assertEquals(null, p.events().get(0).instructorId());

        // Features: e0 needs f0 and two seats -> room 0 only; e3 needs f1 -> room 1 only.
        assertArrayEquals(new int[] {0}, p.suitableRoomsOf(0));
        assertArrayEquals(new int[] {1, 0}, p.suitableRoomsOf(1));
        assertArrayEquals(new int[] {0}, p.suitableRoomsOf(2));
        assertArrayEquals(new int[] {1}, p.suitableRoomsOf(3));

        // Availability: e1 only on day 0, e2 not in the very last slot.
        assertEquals(45, p.availableSlotsOf(0).length);
        assertArrayEquals(IntStream.range(0, 9).toArray(), p.availableSlotsOf(1));
        assertEquals(44, p.availableSlotsOf(2).length);
        assertFalse(p.isAvailable(2, 44));

        // Precedence: row 1 col 0 = 1 -> e1 before e0; row 2 col 3 = 1 -> e2 before e3.
        assertEquals(2, p.precedenceCount());
        assertEquals(1, p.precedenceBefore(0));
        assertEquals(0, p.precedenceAfter(0));
        assertEquals(2, p.precedenceBefore(1));
        assertEquals(3, p.precedenceAfter(1));
    }

    @Test
    void precedenceDirectionMatchesTheOfficialChecker() throws Exception {
        TimetablingProblem p = Itc2007Loader.load(fixture());
        // e1 (slot 0) before e0 (slot 1), e2 (slot 2) before e3 (slot 3): satisfied.
        int[] rooms = {0, 1, 0, 1};
        assertEquals(0, CostModel.evaluate(p, new int[] {1, 0, 2, 3}, rooms).precedenceViolations());
        // Reversing both pairs violates both.
        assertEquals(2, CostModel.evaluate(p, new int[] {0, 1, 3, 2}, rooms).precedenceViolations());
        assertTrue(CostModel.evaluate(p, new int[] {1, 0, 2, 3}, rooms).feasible());
    }

    @Test
    void acceptsTheTwoThousandTwoFormatWithoutTheNewMatrices() throws Exception {
        TimetablingProblem p = Itc2007Loader.parse("2 1 1 1\n5\n1 1\n1\n0\n1\n");
        assertEquals(2, p.eventCount());
        assertEquals(0, p.precedenceCount());
        assertEquals(45, p.availableSlotsOf(1).length);
        assertArrayEquals(new int[] {0}, p.suitableRoomsOf(0));
    }

    @Test
    void rejectsMalformedInput() {
        String head = "1 1 0 1\n5\n1\n";
        String availability = "1 ".repeat(45) + "\n";
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse("1 1 0"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse("0 1 0 1\n5\n"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse("1 1 0 1\n5\n2\n"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse("1 1 0 1\n5\nx\n"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse("1 1 -1 1\n5\n"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse("1 1 0 1\n99999999999\n"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse(head + availability + "2\n"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse(head + availability + "1\n"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse(head + availability + "0 7\n"));
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.parse(head + "1 1\n"));
    }

    @Test
    void writesSolutionsInCompetitionFormat() {
        String sln = Itc2007Loader.toSolution(new int[] {3, CostModel.UNASSIGNED, 44},
                new int[] {7, CostModel.UNASSIGNED, 0});
        assertEquals("3 7\n-1 -1\n44 0\n", sln);
        assertThrows(IllegalArgumentException.class, () -> Itc2007Loader.toSolution(new int[1], new int[2]));
    }

    @Test
    void missingFileIsAnIoError(@org.junit.jupiter.api.io.TempDir Path dir) {
        assertThrows(IOException.class, () -> Itc2007Loader.load(dir.resolve("absent.tim")));
        assertTrue(Files.isDirectory(dir));
    }
}
