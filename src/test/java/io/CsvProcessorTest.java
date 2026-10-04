package io;

import models.Course;
import models.Department;
import models.Enrollment;
import models.Grade;
import models.Professor;
import models.Student;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvProcessorTest {

    @TempDir
    Path dir;

    private Path write(String name, String... lines) throws Exception {
        Path file = dir.resolve(name);
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
        return file;
    }

    private static Student student(String id, String first, String dept) {
        Student s = new Student(id, first, "Doe", first.toLowerCase() + "@uni.edu", null, id, "CS",
                Student.AcademicYear.JUNIOR);
        s.setDepartmentId(dept);
        s.setEnrollmentDate(LocalDate.of(2023, 9, 1));
        return s;
    }

    private static Course course(String id, String description) {
        Course c = new Course(id, "CS101", "Intro, Part 1", description, 3, "D1", "P1",
                Course.DifficultyLevel.INTERMEDIATE, "Fall", 2024);
        c.setMaxEnrollment(42);
        return c;
    }

    private static <T> Map<String, T> mapOf(List<T> items, Function<T, String> key) {
        return CsvProcessor.createLookupMap(items, key);
    }

    @Nested
    class Students {

        @Test
        void roundTripPreservesFieldsIncludingQuotedCommasAndQuotes() throws Exception {
            Student plain = student("S1", "Ann", "D1");
            Student tricky = student("S2", "Bob", "D1");
            tricky.setLastName("O\"Neil, Jr.");
            Path file = dir.resolve("nested/students.csv");

            CsvProcessor.writeStudents(List.of(plain, tricky), file);
            List<Student> read = CsvProcessor.readStudents(file, Map.of("D1", new Department()));

            assertThat(Files.readAllLines(file).get(0)).isEqualTo(String.join(",", CsvProcessor.STUDENT_HEADERS));
            assertThat(read).hasSize(2);
            Student back = read.get(1);
            assertThat(back.getStudentId()).isEqualTo("S2");
            assertThat(back.getUserId()).isEqualTo("S2");
            assertThat(back.getLastName()).isEqualTo("O\"Neil, Jr.");
            assertThat(back.getEmail()).isEqualTo("bob@uni.edu");
            assertThat(back.getDepartmentId()).isEqualTo("D1");
            assertThat(back.getMajor()).isEqualTo("CS");
            assertThat(back.getAcademicYear()).isEqualTo(Student.AcademicYear.JUNIOR);
            assertThat(back.getEnrollmentDate()).isEqualTo(LocalDate.of(2023, 9, 1));
        }

        @Test
        void malformedShortAndBlankLinesAreSkipped() throws Exception {
            Path file = write("s.csv",
                    String.join(",", CsvProcessor.STUDENT_HEADERS),
                    "S1,Ann,Doe,ann@uni.edu,,CS,SENIOR,2022-01-15",
                    "   ",
                    "S2,too,short",
                    "S3,Bad,Year,b@uni.edu,D1,CS,NOT_A_YEAR,2022-01-15",
                    "S4,Bad,Date,b@uni.edu,D1,CS,SENIOR,15/01/2022");

            List<Student> read = CsvProcessor.readStudents(file, Map.of());

            assertThat(read).extracting(Student::getStudentId).containsExactly("S1");
            assertThat(read.get(0).getDepartmentId()).isNull();
        }

        @Test
        void emptyFileYieldsEmptyList() throws Exception {
            Path file = write("empty.csv");
            assertThat(CsvProcessor.readStudents(file, Map.of())).isEmpty();
        }

        @Test
        void missingFileThrows() {
            assertThatThrownBy(() -> CsvProcessor.readStudents(dir.resolve("nope.csv"), Map.of()))
                    .isInstanceOf(java.io.IOException.class);
        }
    }

    @Nested
    class Professors {

        @Test
        void roundTrip() throws Exception {
            Professor p = new Professor("P1", "Alan", "Turing", "alan@uni.edu", null, "P1", "D1",
                    Professor.AcademicRank.ASSOCIATE, "Computability, Logic");
            p.setOfficeLocation("Room 1");
            p.setYearsOfExperience(12);
            Path file = dir.resolve("p.csv");

            CsvProcessor.writeProfessors(List.of(p), file);
            List<Professor> read = CsvProcessor.readProfessors(file, Map.of());

            assertThat(read).singleElement().satisfies(back -> {
                assertThat(back.getProfessorId()).isEqualTo("P1");
                assertThat(back.getDepartmentId()).isEqualTo("D1");
                assertThat(back.getAcademicRank()).isEqualTo(Professor.AcademicRank.ASSOCIATE);
                assertThat(back.getSpecialization()).isEqualTo("Computability, Logic");
                assertThat(back.getOfficeLocation()).isEqualTo("Room 1");
                assertThat(back.getYearsOfExperience()).isEqualTo(12);
            });
        }

        @Test
        void invalidRowsAreDropped() throws Exception {
            Path file = write("p.csv", "header",
                    "P1,A,B,a@b.c,D1,ASSOCIATE,Spec,Room,notanumber",
                    "P2,A,B,a@b.c,D1",
                    "");
            assertThat(CsvProcessor.readProfessors(file, Map.of())).isEmpty();
            assertThat(CsvProcessor.readProfessors(write("e.csv"), Map.of())).isEmpty();
        }
    }

    @Nested
    class Courses {

        @Test
        void roundTripResolvesFieldsAndBlankProfessorBecomesNull() throws Exception {
            Course withProf = course("C1", "Basics");
            Course noProf = course("C2", "More");
            noProf.setProfessorId(null);
            Path file = dir.resolve("c.csv");

            CsvProcessor.writeCourses(List.of(withProf, noProf), file);
            List<Course> read = CsvProcessor.readCourses(file, Map.of(), Map.of());

            assertThat(read).hasSize(2);
            Course back = read.get(0);
            assertThat(back.getCourseId()).isEqualTo("C1");
            assertThat(back.getCourseName()).isEqualTo("Intro, Part 1");
            assertThat(back.getCredits()).isEqualTo(3);
            assertThat(back.getProfessorId()).isEqualTo("P1");
            assertThat(back.getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.INTERMEDIATE);
            assertThat(back.getSemester()).isEqualTo("Fall");
            assertThat(back.getYear()).isEqualTo(2024);
            assertThat(back.getMaxEnrollment()).isEqualTo(42);
            assertThat(read.get(1).getProfessorId()).isNull();
        }

        // Regression: values containing line breaks are written quoted across several physical
        // lines, but the readers split records per line, so such records were silently dropped.
        @Test
        void descriptionWithLineBreaksSurvivesRoundTrip() throws Exception {
            Course multiLine = course("C1", "Line one\nLine \"two\", with comma");
            Course after = course("C2", "Plain");
            Path file = dir.resolve("c.csv");

            CsvProcessor.writeCourses(List.of(multiLine, after), file);
            List<Course> read = CsvProcessor.readCourses(file, Map.of(), Map.of());

            assertThat(read).extracting(Course::getCourseId).containsExactly("C1", "C2");
            assertThat(read.get(0).getDescription()).isEqualTo("Line one\nLine \"two\", with comma");
        }

        @Test
        void invalidCreditsDropRow() throws Exception {
            Path file = write("c.csv", "h",
                    "C1,CS1,Name,Desc,99,D1,P1,BEGINNER,Fall,2024,30",
                    "C2,CS2,Name,Desc,3,D1,P1,BEGINNER,Fall,2024");
            assertThat(CsvProcessor.readCourses(file, Map.of(), Map.of())).isEmpty();
            assertThat(CsvProcessor.readCourses(write("e.csv"), Map.of(), Map.of())).isEmpty();
        }
    }

    @Nested
    class Departments {

        @Test
        void roundTripWithOptionalFields() throws Exception {
            Department full = new Department("D1", "cs", "Computer Science", "Desc, with comma", "Bldg A");
            full.setHeadOfDepartmentId("P1");
            full.setEstablishedYear("1965");
            Department bare = new Department("D2", "MA", "Math", "", "Bldg B");
            Path file = dir.resolve("d.csv");

            CsvProcessor.writeDepartments(List.of(full, bare), file);
            List<Department> read = CsvProcessor.readDepartments(file);

            assertThat(read).hasSize(2);
            assertThat(read.get(0).getDepartmentCode()).isEqualTo("CS");
            assertThat(read.get(0).getDescription()).isEqualTo("Desc, with comma");
            assertThat(read.get(0).getHeadOfDepartmentId()).isEqualTo("P1");
            assertThat(read.get(0).getEstablishedYear()).isEqualTo("1965");
            assertThat(read.get(1).getHeadOfDepartmentId()).isNull();
            assertThat(read.get(1).getEstablishedYear()).isNull();
        }

        @Test
        void rowsViolatingModelConstraintsAreDropped() throws Exception {
            Path file = write("d.csv", "h", ",CODE,Name,Desc,Loc,,", "D1,CODE");
            assertThat(CsvProcessor.readDepartments(file)).isEmpty();
            assertThat(CsvProcessor.readDepartments(write("e.csv"))).isEmpty();
        }
    }

    @Nested
    class Enrollments {

        private final Map<String, Student> students = Map.of("S1", student("S1", "Ann", "D1"));
        private final Map<String, Course> courses = Map.of("C1", course("C1", "x"));

        @Test
        void roundTripTruncatesDateTimeToDate() throws Exception {
            Enrollment e = new Enrollment("E1", "S1", "C1", "Fall", 2024);
            e.setEnrollmentDate(LocalDateTime.of(2024, 8, 20, 13, 45));
            e.setStatus(Enrollment.EnrollmentStatus.DROPPED);
            Path file = dir.resolve("e.csv");

            CsvProcessor.writeEnrollments(List.of(e), file);
            List<Enrollment> read = CsvProcessor.readEnrollments(file, students, courses);

            assertThat(read).singleElement().satisfies(back -> {
                assertThat(back.getEnrollmentId()).isEqualTo("E1");
                assertThat(back.getEnrollmentDate()).isEqualTo(LocalDateTime.of(2024, 8, 20, 0, 0));
                assertThat(back.getSemester()).isEqualTo("Fall");
                assertThat(back.getYear()).isEqualTo(2024);
                assertThat(back.getStatus()).isEqualTo(Enrollment.EnrollmentStatus.DROPPED);
            });
        }

        @Test
        void rowsWithUnknownStudentOrCourseOrBadDataAreDropped() throws Exception {
            Path file = write("e.csv", "h",
                    "E1,SX,C1,2024-01-01,Fall,2024,ENROLLED",
                    "E2,S1,CX,2024-01-01,Fall,2024,ENROLLED",
                    "E3,S1,C1,bad-date,Fall,2024,ENROLLED",
                    "E4,S1,C1",
                    "E5,S1,C1,2024-01-01,Fall,2024,ENROLLED");
            assertThat(CsvProcessor.readEnrollments(file, students, courses))
                    .extracting(Enrollment::getEnrollmentId).containsExactly("E5");
            assertThat(CsvProcessor.readEnrollments(write("x.csv"), students, courses)).isEmpty();
        }
    }

    @Nested
    class Grades {

        private final Map<String, Student> students = Map.of("S1", student("S1", "Ann", "D1"));
        private final Map<String, Course> courses = Map.of("C1", course("C1", "x"));

        @Test
        void roundTripKeepsPointsDatesAndQuotedFeedback() throws Exception {
            Grade g = new Grade("G1", "E1", "S1", "C1", "Midterm", Grade.GradeComponent.EXAM);
            g.setPointsPossible(50);
            g.setPointsEarned(45);
            g.setSemester("Fall");
            g.setYear(2024);
            g.setDateGraded(LocalDateTime.of(2024, 10, 1, 9, 30, 15));
            g.setFeedback("Good, \"solid\" work");
            Grade ungraded = new Grade("G2", "E1", "S1", "C1", "Quiz", Grade.GradeComponent.QUIZ);
            ungraded.setPointsPossible(10);
            Path file = dir.resolve("g.csv");

            CsvProcessor.writeGrades(List.of(g, ungraded), file);
            List<Grade> read = CsvProcessor.readGrades(file, students, courses);

            assertThat(read).hasSize(2);
            Grade back = read.get(0);
            assertThat(back.getPointsEarned()).isEqualTo(45.0);
            assertThat(back.getPointsPossible()).isEqualTo(50.0);
            assertThat(back.getComponent()).isEqualTo(Grade.GradeComponent.EXAM);
            assertThat(back.getDateGraded()).isEqualTo(LocalDateTime.of(2024, 10, 1, 9, 30, 15));
            assertThat(back.getFeedback()).isEqualTo("Good, \"solid\" work");
            assertThat(back.getSemester()).isEqualTo("Fall");
            assertThat(back.getYear()).isEqualTo(2024);
            assertThat(read.get(1).getPointsEarned()).isEqualTo(-1.0);
            assertThat(read.get(1).getDateGraded()).isNull();
        }

        @Test
        void feedbackColumnIsOptionalAndBadRowsAreDropped() throws Exception {
            Path file = write("g.csv", "h",
                    "G1,E1,S1,C1,HW,HOMEWORK,8.0,10.0,Fall,2024,",
                    "G2,E1,SX,C1,HW,HOMEWORK,8.0,10.0,Fall,2024,",
                    "G3,E1,S1,CX,HW,HOMEWORK,8.0,10.0,Fall,2024,",
                    "G4,E1,S1,C1,HW,NOPE,8.0,10.0,Fall,2024,",
                    "G5,E1,S1");
            List<Grade> read = CsvProcessor.readGrades(file, students, courses);
            assertThat(read).extracting(Grade::getGradeId).containsExactly("G1");
            assertThat(read.get(0).getFeedback()).isEmpty();
            assertThat(CsvProcessor.readGrades(write("x.csv"), students, courses)).isEmpty();
        }
    }

    @Nested
    class Utilities {

        @Test
        void lookupMapKeepsFirstOnDuplicateKeys() {
            Map<Integer, String> map = CsvProcessor.createLookupMap(List.of("a", "bb", "c"), String::length);
            assertThat(map).containsExactlyInAnyOrderEntriesOf(Map.of(1, "a", 2, "bb"));
        }

        @Test
        void batchesSkipHeaderAndBlankLinesAndFlushRemainder() throws Exception {
            Path file = write("b.csv", "h", "1", "", "2", "3", "4", "5");
            List<List<Integer>> batches = new ArrayList<>();

            CsvProcessor.processCsvInBatches(file, 2,
                    lines -> lines.stream().map(Integer::parseInt).toList(), batches::add);

            assertThat(batches).containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
        }

        @Test
        void validateHeadersIsCaseInsensitiveAndTrims() throws Exception {
            String[] expected = {"id", "name"};
            assertThat(CsvProcessor.validateHeaders(write("a.csv", "ID , Name", "1,x"), expected)).isTrue();
            assertThat(CsvProcessor.validateHeaders(write("b.csv", "id,other"), expected)).isFalse();
            assertThat(CsvProcessor.validateHeaders(write("c.csv", "id"), expected)).isFalse();
            assertThat(CsvProcessor.validateHeaders(write("d.csv"), expected)).isFalse();
        }

        @Test
        void statisticsCountDataAndEmptyLines() throws Exception {
            CsvProcessor.CsvStatistics stats = CsvProcessor.getCsvStatistics(write("s.csv", "h", "a", " ", "b", ""));
            assertThat(stats.getTotalLines()).isEqualTo(5);
            assertThat(stats.getHeaderLines()).isEqualTo(1);
            assertThat(stats.getDataLines()).isEqualTo(2);
            assertThat(stats.getEmptyLines()).isEqualTo(2);
            assertThat(stats.toString()).isEqualTo("CsvStatistics{total=5, headers=1, data=2, empty=2}");

            CsvProcessor.CsvStatistics empty = CsvProcessor.getCsvStatistics(write("e.csv"));
            assertThat(empty.getTotalLines()).isZero();
        }

        @Test
        void mergeSkipsHeadersAndMissingFiles() throws Exception {
            Path a = write("a.csv", "x,y", "1,2");
            Path b = write("b.csv", "x,y", "3,4", "5,6");
            Path empty = write("empty.csv");
            Path out = dir.resolve("out/merged.csv");

            CsvProcessor.mergeCsvFiles(List.of(a, dir.resolve("missing.csv"), empty, b), out, new String[]{"x", "y"});

            assertThat(Files.readAllLines(out)).containsExactly("x,y", "1,2", "3,4", "5,6");
        }

        @Test
        void splitRepeatsHeaderInEachChunk() throws Exception {
            Path file = write("big.csv", "h", "1", "2", "3", "4", "5");

            List<Path> parts = CsvProcessor.splitCsvFile(file, 2, "chunk");

            assertThat(parts).extracting(p -> p.getFileName().toString())
                    .containsExactly("chunk_part1.csv", "chunk_part2.csv", "chunk_part3.csv");
            assertThat(Files.readAllLines(parts.get(0))).containsExactly("h", "1", "2");
            assertThat(Files.readAllLines(parts.get(2))).containsExactly("h", "5");
            assertThat(CsvProcessor.splitCsvFile(write("e.csv"), 2, "x")).isEmpty();
        }

        @Test
        void convertToTsvUnquotesValues() throws Exception {
            Path csv = write("in.csv", "a,b", "\"x, y\",\"say \"\"hi\"\"\"");
            Path tsv = dir.resolve("out.tsv");

            CsvProcessor.convertCsvToTsv(csv, tsv);

            assertThat(Files.readAllLines(tsv)).containsExactly("a\tb", "x, y\tsay \"hi\"");
        }

        @Test
        void customFormatExportEscapesValuesAndNulls() throws Exception {
            Path out = dir.resolve("custom.csv");
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"plain", "with,comma", null});
            CsvProcessor.exportDataWithCustomFormat(out, new String[]{"a", "b", "c"}, rows, ",");
            assertThat(Files.readAllLines(out)).containsExactly("a,b,c", "plain,\"with,comma\",");
        }

        // Regression: values were only quoted when they contained a comma, so a value containing a
        // custom delimiter (e.g. ';') corrupted the column layout.
        @Test
        void customFormatExportQuotesValuesContainingTheCustomDelimiter() throws Exception {
            Path out = dir.resolve("custom.csv");
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"a;b", "c"});
            CsvProcessor.exportDataWithCustomFormat(out, new String[]{"x", "y"}, rows, ";");
            assertThat(Files.readAllLines(out)).containsExactly("x;y", "\"a;b\";c");
        }
    }

    @Nested
    class Validation {

        private final String[] headers = {"id", "name", "age"};

        @Test
        void validFile() throws Exception {
            CsvProcessor.CsvValidationResult result =
                    CsvProcessor.validateCsvFile(write("v.csv", "id,name,age", "1,a,3"), headers, 3);
            assertThat(result.isValid()).isTrue();
            assertThat(result.hasErrors()).isFalse();
            assertThat(result.hasWarnings()).isFalse();
        }

        @Test
        void reportsHeaderMismatchShortRowsAndEmptyLines() throws Exception {
            CsvProcessor.CsvValidationResult result = CsvProcessor.validateCsvFile(
                    write("v.csv", "id,name", "1,a,3", "", "2,b"), headers, 3);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).containsExactly(
                    "Invalid CSV headers. Expected: [id, name, age]",
                    "Insufficient columns at row 4. Expected at least 3, found 2");
            assertThat(result.getWarnings()).containsExactly("Empty line at row 3");
        }

        @Test
        void missingAndEmptyFiles() throws Exception {
            CsvProcessor.CsvValidationResult missing =
                    CsvProcessor.validateCsvFile(dir.resolve("none.csv"), headers, 3);
            assertThat(missing.isValid()).isFalse();
            assertThat(missing.getErrors()).singleElement().asString().startsWith("CSV file does not exist");

            CsvProcessor.CsvValidationResult empty = CsvProcessor.validateCsvFile(write("e.csv"), headers, 3);
            assertThat(empty.getErrors()).containsExactly("CSV file is empty");
        }
    }
}
