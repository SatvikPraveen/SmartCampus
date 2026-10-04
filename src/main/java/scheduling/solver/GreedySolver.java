package scheduling.solver;

import java.util.Arrays;
import java.util.BitSet;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import scheduling.eval.TimetableState;
import scheduling.model.TimetablingProblem;

/**
 * Sequential greedy construction.
 *
 * <p>Events are taken one at a time in an order given by {@link Ordering}. Each event is placed in
 * the (slot, room) pair that minimises the increase of the weighted objective, where the room
 * considered for a slot is the smallest free room that fits (best fit), falling back to the
 * least-damaging alternative when none exists. With {@link Ordering#DSATUR} the next event is
 * chosen dynamically as the one whose already-placed neighbours occupy the most distinct slots
 * (Brélaz's saturation heuristic for graph colouring), ties broken by degree and then by a seeded
 * random key.</p>
 */
public final class GreedySolver implements TimetableSolver {

    /** Order in which events are inserted. */
    public enum Ordering {
        /** Events in input order; isolates the effect of the placement rule. */
        INPUT,
        /** Seeded random permutation. */
        RANDOM,
        /** Static largest-degree-first in the conflict graph (Welsh&ndash;Powell). */
        LARGEST_DEGREE,
        /** Dynamic saturation-degree ordering (DSATUR). */
        DSATUR
    }

    private final Ordering ordering;

    public GreedySolver(Ordering ordering) {
        this.ordering = ordering;
    }

    @Override
    public String name() {
        return "greedy-" + ordering.name().toLowerCase().replace('_', '-');
    }

    @Override
    public TimetableState construct(TimetablingProblem p, long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        TimetableState state = new TimetableState(p);
        int n = p.eventCount();
        double[] tieBreak = new double[n];
        for (int e = 0; e < n; e++) {
            tieBreak[e] = rng.nextDouble();
        }
        if (ordering == Ordering.DSATUR) {
            constructDsatur(p, state, tieBreak);
        } else {
            for (int e : staticOrder(p, tieBreak)) {
                placeBest(p, state, e);
            }
        }
        return state;
    }

    private int[] staticOrder(TimetablingProblem p, double[] tieBreak) {
        Integer[] order = IntStream.range(0, p.eventCount()).boxed().toArray(Integer[]::new);
        switch (ordering) {
            case RANDOM -> Arrays.sort(order, (a, b) -> Double.compare(tieBreak[a], tieBreak[b]));
            case LARGEST_DEGREE -> Arrays.sort(order, (a, b) -> {
                int c = Integer.compare(p.degreeOf(b), p.degreeOf(a));
                return c != 0 ? c : Double.compare(tieBreak[a], tieBreak[b]);
            });
            default -> { }
        }
        return Arrays.stream(order).mapToInt(Integer::intValue).toArray();
    }

    private void constructDsatur(TimetablingProblem p, TimetableState state, double[] tieBreak) {
        int n = p.eventCount();
        BitSet[] neighbourSlots = new BitSet[n];
        for (int e = 0; e < n; e++) {
            neighbourSlots[e] = new BitSet(p.slotCount());
        }
        boolean[] placed = new boolean[n];
        for (int step = 0; step < n; step++) {
            int best = -1;
            for (int e = 0; e < n; e++) {
                if (placed[e]) {
                    continue;
                }
                if (best < 0 || compareDsatur(p, neighbourSlots, tieBreak, e, best) < 0) {
                    best = e;
                }
            }
            placeBest(p, state, best);
            placed[best] = true;
            int slot = state.slotOf(best);
            for (int f : p.neighboursOf(best)) {
                neighbourSlots[f].set(slot);
            }
        }
    }

    private static int compareDsatur(TimetablingProblem p, BitSet[] sat, double[] tie, int a, int b) {
        int c = Integer.compare(sat[b].cardinality(), sat[a].cardinality());
        if (c != 0) {
            return c;
        }
        c = Integer.compare(p.degreeOf(b), p.degreeOf(a));
        return c != 0 ? c : Double.compare(tie[a], tie[b]);
    }

    /** Places {@code e} at the (slot, room) pair with the smallest weighted cost increase. */
    static void placeBest(TimetablingProblem p, TimetableState state, int e) {
        long before = state.weighted(HARD_WEIGHT);
        long bestDelta = Long.MAX_VALUE;
        int bestSlot = 0;
        int bestRoom = 0;
        for (int t = 0; t < p.slotCount(); t++) {
            int room = chooseRoom(p, state, e, t);
            state.assign(e, t, room);
            long delta = state.weighted(HARD_WEIGHT) - before;
            if (delta < bestDelta) {
                bestDelta = delta;
                bestSlot = t;
                bestRoom = room;
            }
        }
        state.assign(e, bestSlot, bestRoom);
    }

    /**
     * Best-fit free room for {@code e} in {@code slot}; if every suitable room is taken, the largest
     * free room (capacity violation) or else the least-occupied suitable room (double booking).
     */
    static int chooseRoom(TimetablingProblem p, TimetableState state, int e, int slot) {
        for (int r : p.suitableRoomsOf(e)) {
            if (state.roomOccupancy(r, slot) == 0 || state.roomOf(e) == r && state.slotOf(e) == slot) {
                return r;
            }
        }
        int largestFree = -1;
        for (int r = 0; r < p.roomCount(); r++) {
            if (state.roomOccupancy(r, slot) == 0
                    && (largestFree < 0 || p.capacityOf(r) > p.capacityOf(largestFree))) {
                largestFree = r;
            }
        }
        if (largestFree >= 0) {
            return largestFree;
        }
        int[] candidates = p.suitableRoomsOf(e).length > 0
                ? p.suitableRoomsOf(e)
                : IntStream.range(0, p.roomCount()).toArray();
        int best = candidates[0];
        for (int r : candidates) {
            if (state.roomOccupancy(r, slot) < state.roomOccupancy(best, slot)) {
                best = r;
            }
        }
        return best;
    }
}
