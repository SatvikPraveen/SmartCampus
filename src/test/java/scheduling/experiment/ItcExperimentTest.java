package scheduling.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ItcExperimentTest {

    private static void copyFixture(Path dir, String name) throws IOException {
        try (InputStream in = ItcExperimentTest.class.getResourceAsStream("/scheduling/itc2007-tiny.tim")) {
            Files.copy(in, dir.resolve(name));
        }
    }

    @Test
    void runsEverySolverOnEveryInstanceAndSeed(@TempDir Path tmp) throws IOException {
        Path data = Files.createDirectories(tmp.resolve("itc"));
        copyFixture(data, "comp-2007-2-1.tim");
        copyFixture(data, "comp-2007-2-2.tim");
        Path out = tmp.resolve("out");
        Path sln = tmp.resolve("sln");
        ExperimentRunner.main(new String[] {
            "--itc", data.toString(), "--instances", "1-2", "--seeds", "2", "--iterations", "300",
            "--threads", "2", "--out", out.toString(), "--solutions", sln.toString()});

        List<String> csv = Files.readAllLines(out.resolve("runs.csv"));
        assertEquals(1 + 2 * 2 * 7, csv.size());
        assertTrue(csv.get(0).startsWith("instance,seed,"));
        assertTrue(csv.get(1).startsWith("comp-2007-2-1,1,4,3,2,2,"), csv.get(1));
        String summary = Files.readString(out.resolve("summary.md"));
        assertTrue(summary.contains("## Overall"));
        assertTrue(summary.contains("sa(greedy-dsatur) - descent(greedy-dsatur)"));
        try (Stream<Path> files = Files.list(sln)) {
            assertEquals(2 * 2 * 7, files.count());
        }
        List<String> solution = Files.readAllLines(sln.resolve("comp-2007-2-1_sa_greedy-dsatur__s1.sln"));
        assertEquals(4, solution.size());
    }

    @Test
    void parsesInstanceRangesAndLists() {
        assertEquals(List.of(1, 2, 3, 7), ItcExperiment.parseInstances("1-3, 7"));
    }

    @Test
    void missingInstanceFileExplainsHowToFetchTheData(@TempDir Path tmp) {
        IOException e = assertThrows(IOException.class,
                () -> ItcExperiment.run(tmp, "5", 1, 10, 1, tmp.resolve("out"), null));
        assertTrue(e.getMessage().contains("fetch-itc2007.sh"));
    }
}
