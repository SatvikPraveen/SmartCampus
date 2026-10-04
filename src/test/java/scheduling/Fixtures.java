package scheduling;

import java.util.List;
import java.util.Set;

import scheduling.model.Event;
import scheduling.model.Room;
import scheduling.model.TimetablingProblem;

/** Hand-built instances with known structure. */
final class Fixtures {

    private Fixtures() {
    }

    /**
     * Four events, two rooms, one day with three periods.
     * <ul>
     *   <li>e0 {a,b}, instructor X</li>
     *   <li>e1 {b,c}, instructor Y &ndash; shares student b with e0</li>
     *   <li>e2 {d}, instructor X &ndash; shares instructor with e0</li>
     *   <li>e3 {e,f,g}, no instructor &ndash; only fits the big room</li>
     * </ul>
     */
    static TimetablingProblem tiny() {
        return new TimetablingProblem(
                List.of(
                        new Event("e0", "X", Set.of("a", "b")),
                        new Event("e1", "Y", Set.of("b", "c")),
                        new Event("e2", "X", Set.of("d")),
                        new Event("e3", null, Set.of("e", "f", "g"))),
                List.of(new Room("small", 2), new Room("big", 3)),
                1, 3);
    }
}
