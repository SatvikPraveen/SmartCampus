/**
 * University course timetabling (UCTT) engine.
 *
 * <p>The package formulates the post-enrolment course timetabling problem as a
 * constrained combinatorial optimisation problem and provides:</p>
 * <ul>
 *   <li>{@link scheduling.model} &ndash; an immutable problem description (events, rooms,
 *       a {@code days &times; periodsPerDay} slot grid) with a precomputed conflict graph;</li>
 *   <li>{@link scheduling.eval} &ndash; an exact, count-based cost model whose incremental
 *       (delta) evaluation is verified against full recomputation;</li>
 *   <li>{@link scheduling.solver} &ndash; a random baseline, sequential greedy construction under
 *       several orderings (input order, largest-degree-first, DSATUR) and simulated annealing;</li>
 *   <li>{@link scheduling.io} &ndash; a reader for the ITC-2007 post-enrolment {@code .tim}
 *       format;</li>
 *   <li>{@link scheduling.experiment} &ndash; a seeded synthetic instance generator and a
 *       reproducible experiment runner that reports bootstrap confidence intervals, on synthetic
 *       or ITC-2007 instances.</li>
 * </ul>
 *
 * <p>Optional room features, per-event slot availability and event precedence (the extra hard
 * constraints of ITC-2007 track 2) are modelled as further hard cost components; without them
 * the model and every solver behave exactly as before.</p>
 *
 * <p>The constraint set follows the structure popularised by the first International
 * Timetabling Competition (post-enrolment track): hard constraints on student/instructor
 * clashes, room double-booking and room capacity, and three student-centred soft constraints
 * (class in the last period of a day, more than two consecutive classes, a single class on a
 * day). The engine is self-contained and does not depend on the rest of the code base, so it
 * can be studied, tested and benchmarked in isolation.</p>
 */
package scheduling;
