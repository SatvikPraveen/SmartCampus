package scheduling.io;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scheduling.model.Event;
import scheduling.model.Precedence;
import scheduling.model.Room;
import scheduling.model.TimetablingProblem;

/**
 * Reader for the {@code .tim} format of the post-enrolment course timetabling track of the
 * Second International Timetabling Competition (ITC-2007, track 2).
 *
 * <p>The file is a whitespace-separated sequence of integers:</p>
 * <ol>
 *   <li>header: number of events, rooms, features and students;</li>
 *   <li>one size per room;</li>
 *   <li>student &times; event attendance matrix of 0/1 (the event index changes fastest);</li>
 *   <li>room &times; feature matrix of 0/1;</li>
 *   <li>event &times; feature matrix of 0/1;</li>
 *   <li>event &times; timeslot availability matrix of 0/1 over 45 slots (5 days &times; 9
 *       periods);</li>
 *   <li>event &times; event precedence matrix: entry {@code (a, b) = 1} means event {@code a} must be
 *       held in an earlier slot than event {@code b}, {@code -1} states the same constraint from
 *       the other side and {@code 0} means none.</li>
 * </ol>
 *
 * <p>Sections 6 and 7 are new in ITC-2007; a file that ends after section 5 (the format of the
 * 2002 competition) is accepted and yields a problem without availability or precedence
 * constraints. Like the official checker {@code checksln3b.cpp}, only the {@code 1} entries of the
 * precedence matrix are read; the matrix is redundant, so the {@code -1} entries carry no extra
 * information on well-formed files. Events, rooms, students and features are named
 * {@code e<i>}, {@code r<i>}, {@code s<i>} and {@code f<i>} after their zero-based position in
 * the file, so solution indices match the competition's {@code .sln} numbering.</p>
 */
public final class Itc2007Loader {

    /** Days of the ITC-2007 week. */
    public static final int DAYS = 5;
    /** Periods per ITC-2007 day. */
    public static final int PERIODS_PER_DAY = 9;

    private Itc2007Loader() {
    }

    /** Parses a {@code .tim} file. */
    public static TimetablingProblem load(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.US_ASCII)) {
            return parse(reader);
        }
    }

    /** Parses {@code .tim} content held in a string. */
    public static TimetablingProblem parse(String content) throws IOException {
        return parse(new StringReader(content));
    }

    /** Parses {@code .tim} content from a reader, which is not closed. */
    public static TimetablingProblem parse(Reader reader) throws IOException {
        IntScanner in = new IntScanner(reader);
        int eventCount = in.nextPositive("number of events");
        int roomCount = in.nextPositive("number of rooms");
        int featureCount = in.next("number of features");
        int studentCount = in.next("number of students");
        if (featureCount < 0 || studentCount < 0) {
            throw new IllegalArgumentException("Feature and student counts must be non-negative");
        }
        int slots = DAYS * PERIODS_PER_DAY;

        int[] roomSizes = new int[roomCount];
        for (int r = 0; r < roomCount; r++) {
            roomSizes[r] = in.nextPositive("size of room " + r);
        }
        List<Set<String>> attendees = new ArrayList<>(eventCount);
        for (int e = 0; e < eventCount; e++) {
            attendees.add(new HashSet<>());
        }
        for (int s = 0; s < studentCount; s++) {
            for (int e = 0; e < eventCount; e++) {
                if (in.nextBit("attendance")) {
                    attendees.get(e).add("s" + s);
                }
            }
        }
        List<Room> rooms = new ArrayList<>(roomCount);
        for (int r = 0; r < roomCount; r++) {
            rooms.add(new Room("r" + r, roomSizes[r], readFeatures(in, featureCount, "room feature")));
        }
        List<Event> events = new ArrayList<>(eventCount);
        for (int e = 0; e < eventCount; e++) {
            events.add(new Event("e" + e, null, attendees.get(e),
                    readFeatures(in, featureCount, "event feature")));
        }

        Map<String, Set<Integer>> availability = new HashMap<>();
        List<Precedence> precedences = new ArrayList<>();
        if (in.hasNext()) {
            for (int e = 0; e < eventCount; e++) {
                Set<Integer> allowed = new HashSet<>();
                for (int t = 0; t < slots; t++) {
                    if (in.nextBit("availability")) {
                        allowed.add(t);
                    }
                }
                if (allowed.size() < slots) {
                    availability.put("e" + e, allowed);
                }
            }
            for (int a = 0; a < eventCount; a++) {
                for (int b = 0; b < eventCount; b++) {
                    int v = in.next("precedence");
                    if (v < -1 || v > 1) {
                        throw new IllegalArgumentException("Precedence entries must be -1, 0 or 1, got " + v);
                    }
                    if (v == 1) {
                        if (a == b) {
                            throw new IllegalArgumentException("Event " + a + " cannot precede itself");
                        }
                        precedences.add(new Precedence("e" + a, "e" + b));
                    }
                }
            }
        }
        if (in.hasNext()) {
            throw new IllegalArgumentException("Unexpected trailing data after the precedence matrix");
        }
        return new TimetablingProblem(events, rooms, DAYS, PERIODS_PER_DAY, availability, precedences);
    }

    /**
     * Formats a timetable in the competition's {@code .sln} format: one line per event, in file
     * order, holding the slot and the room index, or {@code -1 -1} for an unplaced event. The
     * output can be checked with the official {@code checksln3b} program.
     */
    public static String toSolution(int[] slots, int[] rooms) {
        if (slots.length != rooms.length) {
            throw new IllegalArgumentException("slots and rooms must have the same length");
        }
        StringBuilder sb = new StringBuilder();
        for (int e = 0; e < slots.length; e++) {
            boolean placed = slots[e] >= 0;
            sb.append(placed ? slots[e] : -1).append(' ').append(placed ? rooms[e] : -1).append('\n');
        }
        return sb.toString();
    }

    private static Set<String> readFeatures(IntScanner in, int featureCount, String what) throws IOException {
        Set<String> features = new HashSet<>();
        for (int f = 0; f < featureCount; f++) {
            if (in.nextBit(what)) {
                features.add("f" + f);
            }
        }
        return features;
    }

    /** Minimal allocation-free integer tokenizer; the instance files have millions of tokens. */
    private static final class IntScanner {

        private final Reader reader;
        private int lookahead = -2;

        IntScanner(Reader reader) {
            this.reader = reader;
        }

        private int peek() throws IOException {
            if (lookahead == -2) {
                lookahead = reader.read();
            }
            return lookahead;
        }

        private void skipWhitespace() throws IOException {
            while (peek() != -1 && Character.isWhitespace(peek())) {
                lookahead = -2;
            }
        }

        boolean hasNext() throws IOException {
            skipWhitespace();
            return peek() != -1;
        }

        int next(String what) throws IOException {
            skipWhitespace();
            if (peek() == -1) {
                throw new IllegalArgumentException("Unexpected end of file while reading " + what);
            }
            boolean negative = false;
            if (peek() == '-') {
                negative = true;
                lookahead = -2;
            }
            long value = 0;
            int digits = 0;
            while (peek() >= '0' && peek() <= '9') {
                value = value * 10 + (peek() - '0');
                if (value > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Number too large while reading " + what);
                }
                digits++;
                lookahead = -2;
            }
            if (digits == 0 || peek() != -1 && !Character.isWhitespace(peek())) {
                throw new IllegalArgumentException("Malformed integer while reading " + what);
            }
            return (int) (negative ? -value : value);
        }

        int nextPositive(String what) throws IOException {
            int v = next(what);
            if (v <= 0) {
                throw new IllegalArgumentException(what + " must be positive, got " + v);
            }
            return v;
        }

        boolean nextBit(String what) throws IOException {
            int v = next(what);
            if (v != 0 && v != 1) {
                throw new IllegalArgumentException(what + " entries must be 0 or 1, got " + v);
            }
            return v == 1;
        }
    }
}
