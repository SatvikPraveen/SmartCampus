package io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Course;
import models.Department;
import models.Enrollment;
import models.Grade;
import models.Professor;
import models.Student;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonProcessorTest {

    @TempDir
    Path dir;

    /** Simple bean used for generic (de)serialization. */
    public static class Item {
        private String name;
        private int qty;
        private LocalDate date;

        public Item() { }

        Item(String name, int qty, LocalDate date) {
            this.name = name;
            this.qty = qty;
            this.date = date;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    /** Output type for transformations. */
    public static class Label {
        private String text;

        public Label() { }

        Label(String text) { this.text = text; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    private Path write(String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private static Student student(String id) {
        Student s = new Student(id, "Ann", "Lee", id.toLowerCase() + "@uni.edu", "555", id, "CS",
                Student.AcademicYear.SENIOR);
        s.setDepartmentId("D1");
        s.setEnrollmentDate(LocalDate.of(2021, 9, 1));
        return s;
    }

    @Nested
    class StringConversions {

        @Test
        void roundTripWithJavaTimeAsIsoString() throws Exception {
            String json = JsonProcessor.toJson(new Item("pen", 3, LocalDate.of(2024, 2, 29)));

            assertThat(json).contains("\"date\" : \"2024-02-29\"").contains(System.lineSeparator());
            Item back = JsonProcessor.fromJson(json, Item.class);
            assertThat(back.getName()).isEqualTo("pen");
            assertThat(back.getQty()).isEqualTo(3);
            assertThat(back.getDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        }

        @Test
        void typeReferenceDeserialization() throws Exception {
            List<Item> items = JsonProcessor.fromJson("[{\"name\":\"a\",\"qty\":1},{\"name\":\"b\",\"qty\":2}]",
                    new TypeReference<List<Item>>() { });
            assertThat(items).extracting(Item::getName).containsExactly("a", "b");
        }

        @Test
        void compactHasNoLineBreaksWhilePrettyDoes() throws Exception {
            Item item = new Item("x", 1, LocalDate.of(2020, 1, 1));
            assertThat(JsonProcessor.toCompactJson(item))
                    .isEqualTo("{\"name\":\"x\",\"qty\":1,\"date\":\"2020-01-01\"}");
            assertThat(JsonProcessor.toPrettyJson(item)).contains("\n").contains("\"qty\" : 1");
        }

        @Test
        void malformedJsonThrows() {
            assertThatThrownBy(() -> JsonProcessor.fromJson("{not json", Item.class))
                    .isInstanceOf(IOException.class);
        }
    }

    @Nested
    class Files_ {

        @Test
        void writeCreatesParentDirectoriesAndReadsBack() throws Exception {
            Path file = dir.resolve("a/b/item.json");
            JsonProcessor.writeToJsonFile(new Item("n", 7, null), file);

            assertThat(JsonProcessor.readFromJsonFile(file, Item.class).getQty()).isEqualTo(7);
            assertThat(JsonProcessor.readFromJsonFile(file, new TypeReference<Map<String, Object>>() { }))
                    .containsEntry("name", "n");
        }

        @Test
        void readingMissingFileThrowsFileNotFound() {
            Path missing = dir.resolve("missing.json");
            assertThatThrownBy(() -> JsonProcessor.readFromJsonFile(missing, Item.class))
                    .isInstanceOf(FileNotFoundException.class).hasMessageContaining("missing.json");
            assertThatThrownBy(() -> JsonProcessor.readFromJsonFile(missing, new TypeReference<List<Item>>() { }))
                    .isInstanceOf(FileNotFoundException.class);
        }
    }

    @Nested
    class EntityRoundTrips {

        @Test
        void students() throws Exception {
            Path file = dir.resolve("students.json");
            JsonProcessor.writeStudents(List.of(student("S1"), student("S2")), file);

            List<Student> back = JsonProcessor.readStudents(file);

            assertThat(back).extracting(Student::getStudentId).containsExactly("S1", "S2");
            assertThat(back.get(0).getEmail()).isEqualTo("s1@uni.edu");
            assertThat(back.get(0).getAcademicYear()).isEqualTo(Student.AcademicYear.SENIOR);
            assertThat(back.get(0).getEnrollmentDate()).isEqualTo(LocalDate.of(2021, 9, 1));
        }

        @Test
        void professors() throws Exception {
            Professor p = new Professor("P1", "Grace", "Hopper", "grace@uni.edu", null, "P1", "D1",
                    Professor.AcademicRank.ASSOCIATE, "Compilers");
            Path file = dir.resolve("professors.json");
            JsonProcessor.writeProfessors(List.of(p), file);

            assertThat(JsonProcessor.readProfessors(file)).singleElement().satisfies(back -> {
                assertThat(back.getProfessorId()).isEqualTo("P1");
                assertThat(back.getSpecialization()).isEqualTo("Compilers");
                assertThat(back.getAcademicRank()).isEqualTo(Professor.AcademicRank.ASSOCIATE);
            });
        }

        @Test
        void courses() throws Exception {
            Course c = new Course("C1", "CS101", "Intro", "Desc", 3, "D1", "P1",
                    Course.DifficultyLevel.ADVANCED, "Fall", 2024);
            Path file = dir.resolve("courses.json");
            JsonProcessor.writeCourses(List.of(c), file);

            assertThat(JsonProcessor.readCourses(file)).singleElement().satisfies(back -> {
                assertThat(back.getCourseCode()).isEqualTo("CS101");
                assertThat(back.getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.ADVANCED);
                assertThat(back.getCredits()).isEqualTo(3);
            });
        }

        @Test
        void departments() throws Exception {
            Department d = new Department("D1", "CS", "Computer Science", "Desc", "Bldg");
            Path file = dir.resolve("departments.json");
            JsonProcessor.writeDepartments(List.of(d), file);

            assertThat(JsonProcessor.readDepartments(file)).singleElement()
                    .satisfies(back -> assertThat(back.getDepartmentName()).isEqualTo("Computer Science"));
        }

        @Test
        void enrollments() throws Exception {
            Enrollment e = new Enrollment("E1", "S1", "C1", "Fall", 2024);
            e.setEnrollmentDate(LocalDateTime.of(2024, 8, 1, 10, 0));
            Path file = dir.resolve("enrollments.json");
            JsonProcessor.writeEnrollments(List.of(e), file);

            assertThat(JsonProcessor.readEnrollments(file)).singleElement().satisfies(back -> {
                assertThat(back.getEnrollmentId()).isEqualTo("E1");
                assertThat(back.getEnrollmentDate()).isEqualTo(LocalDateTime.of(2024, 8, 1, 10, 0));
            });
        }

        @Test
        void grades() throws Exception {
            Grade g = new Grade("G1", "E1", "S1", "C1", "Final", Grade.GradeComponent.EXAM);
            g.setPointsPossible(100);
            g.setPointsEarned(88);
            Path file = dir.resolve("grades.json");
            JsonProcessor.writeGrades(List.of(g), file);

            assertThat(JsonProcessor.readGrades(file)).singleElement().satisfies(back -> {
                assertThat(back.getPointsEarned()).isEqualTo(88.0);
                assertThat(back.getPointsPossible()).isEqualTo(100.0);
            });
        }

        @Test
        void universityExportImport() throws Exception {
            Path file = dir.resolve("export.json");
            Department d = new Department("D1", "CS", "Computer Science", "Desc", "Bldg");

            JsonProcessor.exportUniversityData(file, List.of(student("S1")), List.of(), List.of(),
                    List.of(d), List.of(), List.of());
            JsonProcessor.UniversityDataExport back = JsonProcessor.importUniversityData(file);

            assertThat(back.getVersion()).isEqualTo("1.0");
            assertThat(back.getExportDate()).isNotNull();
            assertThat(back.getStudents()).extracting(Student::getStudentId).containsExactly("S1");
            assertThat(back.getDepartments()).hasSize(1);
            assertThat(back.getProfessors()).isEmpty();
            assertThat(back.getCourses()).isEmpty();
            assertThat(back.getEnrollments()).isEmpty();
            assertThat(back.getGrades()).isEmpty();
        }
    }

    @Nested
    class TreeOperations {

        @Test
        void extractFieldValuesWalksArraysAndNestedObjects() throws Exception {
            Path file = write("t.json", """
                    [{"name":"a","dept":{"name":"x","code":"C1"}},
                     {"name":"b","tags":[{"name":"t"}]},
                     {"other":1}]
                    """);

            assertThat(JsonProcessor.extractFieldValues(file, "name")).containsExactly("a", "x", "b", "t");
            assertThat(JsonProcessor.extractFieldValues(file, "dept.code")).containsExactly("C1");
        }

        @Test
        void mergeConcatenatesArraysAndIgnoresMissingAndNonArrayFiles() throws Exception {
            Path a = write("a.json", "[1,2]");
            Path b = write("b.json", "[3]");
            Path obj = write("o.json", "{\"x\":1}");
            Path out = dir.resolve("merged.json");

            JsonProcessor.mergeJsonArrayFiles(List.of(a, dir.resolve("missing.json"), obj, b), out);

            assertThat(new ObjectMapper().readValue(out.toFile(), int[].class)).containsExactly(1, 2, 3);
        }

        @Test
        void filterKeepsMatchingItemsByPath() throws Exception {
            Path in = write("in.json", """
                    [{"id":1,"s":{"v":"on"}},{"id":2,"s":{"v":"off"}},{"id":3}]
                    """);
            Path out = dir.resolve("out.json");

            JsonProcessor.filterJsonArray(in, out, "s.v", "on");

            List<Map<String, Object>> result = new ObjectMapper().readValue(out.toFile(),
                    new TypeReference<List<Map<String, Object>>>() { });
            assertThat(result).extracting(m -> m.get("id")).containsExactly(1);
        }

        @Test
        void filterRejectsNonArrayInput() throws Exception {
            Path in = write("in.json", "{\"a\":1}");
            assertThatThrownBy(() -> JsonProcessor.filterJsonArray(in, dir.resolve("o.json"), "a", "1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void transformMapsEveryElement() throws Exception {
            Path in = write("in.json", "[{\"name\":\"a\",\"qty\":2},{\"name\":\"b\",\"qty\":5}]");
            Path out = dir.resolve("sub/out.json");

            JsonProcessor.transformJsonArray(in, out, Item.class, Label.class,
                    i -> new Label(i.getName() + "x" + i.getQty()));

            List<Label> labels = JsonProcessor.readFromJsonFile(out, new TypeReference<List<Label>>() { });
            assertThat(labels).extracting(Label::getText).containsExactly("ax2", "bx5");
        }

        @Test
        void transformOfMissingFileThrows() {
            assertThatThrownBy(() -> JsonProcessor.transformJsonArray(dir.resolve("nope.json"),
                    dir.resolve("o.json"), Item.class, Label.class, i -> new Label("")))
                    .isInstanceOf(FileNotFoundException.class);
        }

        @Test
        void schemaListsDeclaredFields() throws Exception {
            Path schema = dir.resolve("schema.json");
            JsonProcessor.generateJsonSchema(Item.class, schema);

            Map<String, Object> read = JsonProcessor.readFromJsonFile(schema, new TypeReference<Map<String, Object>>() { });
            assertThat(read).containsEntry("type", "object").containsEntry("class", "Item");
            assertThat(read.get("properties")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                    .containsOnlyKeys("name", "qty", "date")
                    .containsEntry("qty", Map.of("type", "int", "required", true));
        }
    }

    @Nested
    class Validation {

        @Test
        void validFile() throws Exception {
            JsonProcessor.JsonValidationResult r =
                    JsonProcessor.validateJsonFile(write("v.json", "{\"name\":\"a\",\"qty\":1}"), Item.class);
            assertThat(r.isValid()).isTrue();
            assertThat(r.hasErrors()).isFalse();
            assertThat(r.hasWarnings()).isFalse();
            assertThat(r.getWarnings()).isEmpty();
            assertThat(r).hasToString("JsonValidationResult{valid=true, errors=0, warnings=0}");
        }

        @Test
        void missingFile() {
            JsonProcessor.JsonValidationResult r = JsonProcessor.validateJsonFile(dir.resolve("x.json"), Item.class);
            assertThat(r.isValid()).isFalse();
            assertThat(r.getErrors()).singleElement().asString().startsWith("JSON file does not exist");
        }

        @Test
        void malformedOrMismatchedContent() throws Exception {
            assertThat(JsonProcessor.validateJsonFile(write("bad.json", "{oops"), Item.class).getErrors())
                    .singleElement().asString().startsWith("JSON parsing error");
            assertThat(JsonProcessor.validateJsonFile(write("arr.json", "[1,2]"), Item.class).isValid())
                    .isFalse();
        }
    }

    @Nested
    class Backups {

        @Test
        void backupAndRestoreReproduceFileContents() throws Exception {
            Path a = write("a.json", "[1, 2]");
            Path b = write("b.json", "{\"k\": \"v\\n\"}");
            Path backup = dir.resolve("bk/backup.json");
            Path restoreDir = Files.createDirectory(dir.resolve("restore"));

            JsonProcessor.createJsonBackup(List.of(a, b, dir.resolve("missing.json")), backup);
            JsonProcessor.restoreJsonBackup(backup, restoreDir);

            try (var files = Files.list(restoreDir)) {
                assertThat(files.map(p -> p.getFileName().toString())).containsExactlyInAnyOrder("a.json", "b.json");
            }
            assertThat(Files.readString(restoreDir.resolve("a.json"))).isEqualTo("[1, 2]");
            assertThat(Files.readString(restoreDir.resolve("b.json"))).isEqualTo("{\"k\": \"v\\n\"}");
        }

        @Test
        void restoreWithoutFilesSectionWritesNothing() throws Exception {
            Path backup = write("backup.json", "{\"version\":\"1.0\"}");
            Path restoreDir = Files.createDirectory(dir.resolve("restore"));

            JsonProcessor.restoreJsonBackup(backup, restoreDir);

            try (var files = Files.list(restoreDir)) {
                assertThat(files).isEmpty();
            }
        }

        // Regression: file names inside a backup were resolved against the restore directory
        // unchecked, so a crafted backup could write files outside of it ("../evil.json").
        @Test
        void restoreRejectsFileNamesEscapingTheRestoreDirectory() throws Exception {
            Path backup = write("backup.json", "{\"files\":{\"../evil.json\":\"x\"}}");
            Path restoreDir = Files.createDirectory(dir.resolve("restore"));

            assertThatThrownBy(() -> JsonProcessor.restoreJsonBackup(backup, restoreDir))
                    .hasMessageContaining("evil.json");
            assertThat(dir.resolve("evil.json")).doesNotExist();
        }
    }

    @Nested
    class Config {

        @Test
        void defaults() {
            JsonProcessor.JsonConfig config = new JsonProcessor.JsonConfig();
            assertThat(config.isPrettyPrint()).isTrue();
            assertThat(config.isIncludeNulls()).isFalse();
            assertThat(config.getDateFormat()).isEqualTo("yyyy-MM-dd HH:mm:ss");
        }

        @Test
        void mapperHonoursPrettyPrintAndNullInclusion() throws Exception {
            JsonProcessor.JsonConfig config = new JsonProcessor.JsonConfig();
            config.setPrettyPrint(false);
            Item item = new Item("a", 1, null);

            assertThat(config.createMapper().writeValueAsString(item)).isEqualTo("{\"name\":\"a\",\"qty\":1}");

            config.setIncludeNulls(true);
            config.setPrettyPrint(true);
            config.setDateFormat("yyyy");
            ObjectMapper mapper = config.createMapper();
            assertThat(mapper.writeValueAsString(item)).contains("\"date\" : null").contains("\n");
            assertThat(mapper.writeValueAsString(new Date(0L))).hasSize(6); // quoted 4-digit year

            config.setDateFormat(null);
            assertThat(config.getDateFormat()).isNull();
            assertThat(config.createMapper()).isNotNull();
        }
    }
}
