package scheduling.solver;

import java.util.SplittableRandom;

import scheduling.eval.TimetableState;
import scheduling.model.TimetablingProblem;

/**
 * Simulated annealing over complete timetables.
 *
 * <p>The search starts from the solution of an initial constructive solver and minimises
 * {@code HARD_WEIGHT * hard + soft}. Two neighbourhoods are sampled with equal probability:</p>
 * <ul>
 *   <li><b>relocate</b> &ndash; move one event to a random slot, using the best-fit free room there;</li>
 *   <li><b>swap</b> &ndash; exchange the (slot, room) pairs of two events.</li>
 * </ul>
 * <p>Moves are accepted with the Metropolis criterion {@code exp(-delta / T)}. The temperature
 * decays geometrically from {@code T0} to {@code T0 * finalTemperatureRatio} over the iteration
 * budget. {@code T0} is calibrated per run from the mean absolute <em>soft</em> delta of a sample of
 * random moves so that, initially, an average worsening move is accepted with probability
 * {@link Config#initialAcceptance()}. Calibrating on the soft scale means hard violations are
 * practically never re-introduced once removed, i.e. the search is lexicographic in effect.
 * The initial solver receives the same seed, so for a given seed the starting point is exactly the
 * timetable that solver reports on its own; because the best state seen is returned, the result is
 * never worse than that baseline, which makes paired comparisons meaningful.</p>
 */
public final class SimulatedAnnealingSolver implements TimetableSolver {

    /**
     * Annealing parameters.
     *
     * @param iterations            number of proposed moves
     * @param initialAcceptance     target acceptance probability of an average worsening move at T0
     * @param finalTemperatureRatio ratio {@code T_end / T0}
     * @param calibrationSamples    random moves sampled to estimate the typical soft delta
     */
    public record Config(long iterations, double initialAcceptance, double finalTemperatureRatio,
                         int calibrationSamples) {

        public Config {
            if (iterations < 0) {
                throw new IllegalArgumentException("iterations must be >= 0");
            }
            if (!(initialAcceptance > 0 && initialAcceptance < 1)) {
                throw new IllegalArgumentException("initialAcceptance must be in (0,1)");
            }
            if (!(finalTemperatureRatio > 0 && finalTemperatureRatio < 1)) {
                throw new IllegalArgumentException("finalTemperatureRatio must be in (0,1)");
            }
        }

        public static Config defaults(long iterations) {
            return new Config(iterations, 0.5, 1e-3, 200);
        }
    }

    private final TimetableSolver initial;
    private final Config config;

    public SimulatedAnnealingSolver(TimetableSolver initial, Config config) {
        this.initial = initial;
        this.config = config;
    }

    @Override
    public String name() {
        return "sa(" + initial.name() + ")";
    }

    @Override
    public long iterationsOf(TimetablingProblem problem) {
        return config.iterations();
    }

    @Override
    public TimetableState construct(TimetablingProblem p, long seed) {
        TimetableState current = initial.construct(p, seed);
        SplittableRandom rng = new SplittableRandom(seed ^ 0x5DEECE66DL);
        TimetableState best = current.copy();
        long bestCost = best.weighted(HARD_WEIGHT);
        long currentCost = bestCost;
        if (p.eventCount() < 2 || config.iterations() == 0) {
            return best;
        }

        double t0 = calibrate(p, current, rng);
        double alpha = Math.pow(config.finalTemperatureRatio(), 1.0 / config.iterations());
        double temperature = t0;
        Move move = new Move();
        for (long it = 0; it < config.iterations(); it++, temperature *= alpha) {
            propose(p, current, rng, move);
            move.apply(current);
            long cost = current.weighted(HARD_WEIGHT);
            long delta = cost - currentCost;
            if (delta <= 0 || rng.nextDouble() < Math.exp(-delta / temperature)) {
                currentCost = cost;
                if (cost < bestCost) {
                    bestCost = cost;
                    best = current.copy();
                }
            } else {
                move.undo(current);
            }
        }
        return best;
    }

    private double calibrate(TimetablingProblem p, TimetableState state, SplittableRandom rng) {
        Move move = new Move();
        double sum = 0;
        int counted = 0;
        for (int i = 0; i < config.calibrationSamples(); i++) {
            long hardBefore = state.hard();
            long softBefore = state.soft();
            propose(p, state, rng, move);
            move.apply(state);
            if (state.hard() == hardBefore && state.soft() != softBefore) {
                sum += Math.abs(state.soft() - softBefore);
                counted++;
            }
            move.undo(state);
        }
        double meanDelta = counted == 0 ? 1.0 : sum / counted;
        return -meanDelta / Math.log(config.initialAcceptance());
    }

    private static void propose(TimetablingProblem p, TimetableState s, SplittableRandom rng, Move m) {
        int a = rng.nextInt(p.eventCount());
        if (rng.nextBoolean()) {
            int slot = rng.nextInt(p.slotCount());
            m.set(a, s.slotOf(a), s.roomOf(a), slot, GreedySolver.chooseRoom(p, s, a, slot));
        } else {
            int b = rng.nextInt(p.eventCount() - 1);
            if (b >= a) {
                b++;
            }
            m.setSwap(a, s.slotOf(a), s.roomOf(a), b, s.slotOf(b), s.roomOf(b));
        }
    }

    /** A reversible relocate or swap move. */
    private static final class Move {
        private int a;
        private int b;
        private int aSlot;
        private int aRoom;
        private int bSlot;
        private int bRoom;
        private int newSlot;
        private int newRoom;
        private boolean swap;

        void set(int e, int oldSlot, int oldRoom, int slot, int room) {
            swap = false;
            a = e;
            aSlot = oldSlot;
            aRoom = oldRoom;
            newSlot = slot;
            newRoom = room;
        }

        void setSwap(int e, int eSlot, int eRoom, int f, int fSlot, int fRoom) {
            swap = true;
            a = e;
            aSlot = eSlot;
            aRoom = eRoom;
            b = f;
            bSlot = fSlot;
            bRoom = fRoom;
        }

        void apply(TimetableState s) {
            if (swap) {
                s.assign(a, bSlot, bRoom);
                s.assign(b, aSlot, aRoom);
            } else {
                s.assign(a, newSlot, newRoom);
            }
        }

        void undo(TimetableState s) {
            s.assign(a, aSlot, aRoom);
            if (swap) {
                s.assign(b, bSlot, bRoom);
            }
        }
    }
}
