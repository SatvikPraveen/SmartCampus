package services;

import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportFormat;
import interfaces.Reportable.ReportType;
import interfaces.Searchable.SearchCriteria;
import interfaces.Searchable.SearchCriterion;
import interfaces.Searchable.SearchResult;
import interfaces.Searchable.SortOrder;
import models.Grade;
import models.Grade.GradeComponent;
import models.Professor;
import models.Professor.AcademicRank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProfessorServiceTest {

    private ProfessorService service;
    private int gradeSeq;

    @BeforeEach
    void setUp() {
        service = new ProfessorService();
        gradeSeq = 0;
    }

    private static Professor professor(String id, String first, String last, String dept, AcademicRank rank) {
        return new Professor("U" + id, first, last, first.toLowerCase() + "@campus.edu", null, id, dept, rank, "Spec");
    }

    private Professor add(String id, String first, String last, String dept, AcademicRank rank,
                          double rating, int years, boolean tenured) {
        Professor p = professor(id, first, last, dept, rank);
        p.setTeachingRating(rating);
        p.setYearsOfExperience(years);
        p.setTenured(tenured);
        assertThat(service.addProfessor(p)).isTrue();
        return p;
    }

    private Grade grade(String course, Double points) {
        Grade g = new Grade("G" + (++gradeSeq), "E1", "S1", course, "HW" + gradeSeq, GradeComponent.HOMEWORK);
        g.setPointsPossible(100);
        if (points != null) {
            g.submitAssignment();
            assertThat(g.gradeAssignment(points, "P1", "ok")).isTrue();
        }
        return g;
    }

    /** Ada (CS, FULL, 4.5, 20y, tenured), Bob (CS, ASSISTANT, 3.0, 7y), Cy (MATH, ADJUNCT, 4.0, 2y). */
    private void seedThree() {
        Professor ada = add("P1", "Ada", "Lovelace", "CS", AcademicRank.FULL, 4.5, 20, true);
        ada.setResearchArea("Compilers");
        add("P2", "Bob", "Babbage", "CS", AcademicRank.ASSISTANT, 3.0, 7, false);
        add("P3", "Cy", "Codd", "MATH", AcademicRank.ADJUNCT, 4.0, 2, false);
    }

    private static List<String> ids(List<Professor> professors) {
        return professors.stream().map(Professor::getProfessorId).toList();
    }

    @Nested
    class Crud {

        @Test
        void addGetUpdateAndRemove() {
            Professor p = add("P1", "Ada", "Lovelace", "CS", AcademicRank.FULL, 4.5, 20, true);

            assertThat(service.getProfessorById("P1")).containsSame(p);
            assertThat(service.getAllProfessors()).containsExactly(p);
            assertThat(service.getProfessorCourses("P1")).isEmpty();
            assertThat(service.getProfessorStudents("P1")).isEmpty();
            assertThat(service.getResearchPublicationCount("P1")).isZero();

            Professor updated = professor("P1", "Ada", "King", "CS", AcademicRank.EMERITUS);
            assertThat(service.updateProfessor(updated)).isTrue();
            assertThat(service.getProfessorById("P1")).containsSame(updated);

            assertThat(service.removeProfessor("P1")).isTrue();
            assertThat(service.getProfessorById("P1")).isEmpty();
            assertThat(service.removeProfessor("P1")).isFalse();
            assertThat(service.removeProfessor(" ")).isFalse();
        }

        @Test
        void rejectsInvalidAddsAndUnknownUpdates() {
            add("P1", "Ada", "Lovelace", "CS", AcademicRank.FULL, 4.5, 20, true);

            assertThat(service.addProfessor(null)).isFalse();
            assertThat(service.addProfessor(professor("P1", "Dup", "D", "CS", AcademicRank.FULL))).isFalse();
            assertThat(service.addProfessor(professor("", "Blank", "B", "CS", AcademicRank.FULL))).isFalse();
            assertThat(service.updateProfessor(null)).isFalse();
            assertThat(service.updateProfessor(professor("P9", "X", "Y", "CS", AcademicRank.FULL))).isFalse();
            assertThat(service.getAllProfessors()).hasSize(1);
        }
    }

    @Nested
    class Queries {

        @BeforeEach
        void seed() {
            seedThree();
        }

        @Test
        void byDepartmentIsSortedByName() {
            assertThat(ids(service.getProfessorsByDepartment("CS"))).containsExactly("P2", "P1");
            assertThat(service.getProfessorsByDepartment("ART")).isEmpty();
        }

        @Test
        void byRankTenureAndSeniority() {
            assertThat(ids(service.getProfessorsByRank(AcademicRank.ADJUNCT))).containsExactly("P3");
            assertThat(ids(service.getTenuredProfessors())).containsExactly("P1");
            assertThat(ids(service.getSeniorProfessors())).containsExactly("P1");
            // Bob: untenured assistant with 7 years; Cy is adjunct, Ada already tenured
            assertThat(ids(service.getTenureEligibleProfessors())).containsExactly("P2");
        }

        @Test
        void byRatingSortedDescending() {
            assertThat(ids(service.getProfessorsWithRatingAbove(4.0))).containsExactly("P1", "P3");
            assertThat(ids(service.getTopProfessorsByRating(2))).containsExactly("P1", "P3");
        }

        @Test
        void byExperienceAndResearchArea() {
            assertThat(ids(service.getProfessorsByExperienceRange(2, 7))).containsExactlyInAnyOrder("P2", "P3");
            assertThat(ids(service.getProfessorsByResearchArea("compilers"))).containsExactly("P1");
        }

        @Test
        void predicateAndCriteriaFilters() {
            assertThat(ids(service.filterProfessors(p -> p.getYearsOfExperience() > 5)))
                    .containsExactlyInAnyOrder("P1", "P2");
            assertThat(ids(service.filter(Professor::isTenured))).containsExactly("P1");
            assertThat(ids(service.search((Professor p) -> p.getDepartmentId().equals("MATH")))).containsExactly("P3");
            assertThat(ids(service.findProfessorsByCriteria(Map.of("departmentId", "CS", "tenured", false))))
                    .containsExactly("P2");
            assertThat(service.findProfessorsByCriteria(Map.of("noSuchField", "x"))).isEmpty();
        }
    }

    @Nested
    class CoursesAndStudents {

        @BeforeEach
        void seed() {
            seedThree();
        }

        @Test
        void assignAndRemoveCourses() {
            assertThat(service.assignCourse("P1", "CS101")).isTrue();
            assertThat(service.assignCourse("P1", "CS101")).isFalse();
            assertThat(service.assignCourse("P1", "")).isFalse();
            assertThat(service.assignCourse("NOPE", "CS101")).isFalse();
            assertThat(service.assignCourse("P2", "CS102")).isTrue();

            assertThat(service.getProfessorCourses("P1")).containsExactly("CS101");
            assertThat(ids(service.getProfessorsTeachingCourse("CS101"))).containsExactly("P1");
            assertThat(service.getWorkloadStatistics()).containsEntry("P1", 1).containsEntry("P2", 1).containsEntry("P3", 0);

            assertThat(service.removeCourseAssignment("P1", "CS101")).isTrue();
            assertThat(service.removeCourseAssignment("P1", "CS101")).isFalse();
            assertThat(service.removeCourseAssignment("NOPE", "CS101")).isFalse();
            assertThat(service.getProfessorsTeachingCourse("CS101")).isEmpty();
            assertThat(service.getProfessorCourses("NOPE")).isEmpty();
        }

        @Test
        void addAndRemoveStudents() {
            assertThat(service.addStudent("P1", "S1")).isTrue();
            assertThat(service.addStudent("P1", "S1")).isFalse();
            assertThat(service.addStudent("P1", null)).isFalse();
            assertThat(service.addStudent("NOPE", "S1")).isFalse();
            assertThat(service.getProfessorStudents("P1")).containsExactly("S1");

            assertThat(service.removeStudent("P1", "S1")).isTrue();
            assertThat(service.removeStudent("P1", "S1")).isFalse();
            assertThat(service.removeStudent("NOPE", "S1")).isFalse();
            assertThat(service.getProfessorStudents("NOPE")).isEmpty();
        }
    }

    @Nested
    class Grading {

        @BeforeEach
        void seed() {
            seedThree();
            service.assignCourse("P1", "CS101");
        }

        @Test
        void addAndListCourseGrades() {
            Grade g1 = grade("CS101", 80.0);
            Grade g2 = grade("CS101", 90.0);
            Grade other = grade("CS102", 50.0);
            assertThat(service.addGrade("P1", "CS101", g1)).isTrue();
            assertThat(service.addGrade("P1", "CS101", g2)).isTrue();
            assertThat(service.addGrade("P1", "CS102", other)).isTrue();
            assertThat(service.addGrade("P1", "", g1)).isFalse();
            assertThat(service.addGrade("P1", "CS101", null)).isFalse();
            assertThat(service.addGrade("NOPE", "CS101", g1)).isFalse();

            assertThat(service.getCourseGrades("P1", "CS101")).containsExactlyInAnyOrder(g1, g2);
            assertThat(service.getCourseGrades("NOPE", "CS101")).isEmpty();
            assertThat(service.calculateCourseAverageGrade("P1", "CS101").getAsDouble()).isCloseTo(85.0, within(1e-9));
            assertThat(service.calculateCourseAverageGrade("P1", "CS999")).isEmpty();
        }

        @Test
        void averageIgnoresUngradedWork() {
            // Regression: ungraded grades report a percentage of 0.0, which passed the
            // `percentage >= 0` filter and dragged the course average down.
            service.addGrade("P1", "CS101", grade("CS101", 90.0));
            service.addGrade("P1", "CS101", grade("CS101", null)); // draft, not graded yet

            assertThat(service.calculateCourseAverageGrade("P1", "CS101").getAsDouble()).isCloseTo(90.0, within(1e-9));
        }

        @Test
        void removingCourseAssignmentDropsItsGrades() {
            // Regression: grades are stored under "courseId_gradeId" keys but removal
            // deleted the bare "courseId" key, leaving the grades behind.
            service.addGrade("P1", "CS101", grade("CS101", 70.0));
            service.assignCourse("P1", "CS102");
            Grade kept = grade("CS102", 60.0);
            service.addGrade("P1", "CS102", kept);

            assertThat(service.removeCourseAssignment("P1", "CS101")).isTrue();

            assertThat(service.getCourseGrades("P1", "CS101")).isEmpty();
            assertThat(service.calculateCourseAverageGrade("P1", "CS101")).isEmpty();
            assertThat(service.getCourseGrades("P1", "CS102")).containsExactly(kept);
        }

        @Test
        void courseGradesDoNotLeakAcrossCourseIdsSharingAPrefix() {
            // Regression: prefix matching on "CS1_" also returned grades of course "CS1_LAB".
            Grade lecture = grade("CS1", 80.0);
            Grade lab = grade("CS1_LAB", 40.0);
            service.addGrade("P1", "CS1", lecture);
            service.addGrade("P1", "CS1_LAB", lab);

            assertThat(service.getCourseGrades("P1", "CS1")).containsExactly(lecture);
            assertThat(service.getCourseGrades("P1", "CS1_LAB")).containsExactly(lab);
        }
    }

    @Nested
    class ResearchAndRatings {

        @BeforeEach
        void seed() {
            seedThree();
        }

        @Test
        void publicationsAccumulate() {
            assertThat(service.addResearchPublications("P1", 3)).isTrue();
            assertThat(service.addResearchPublications("P1", 2)).isTrue();
            assertThat(service.addResearchPublications("P1", 0)).isFalse();
            assertThat(service.addResearchPublications("NOPE", 1)).isFalse();

            assertThat(service.getResearchPublicationCount("P1")).isEqualTo(5);
            assertThat(service.getResearchPublicationCount("NOPE")).isZero();
        }

        @ParameterizedTest
        @CsvSource({"0.0,true", "5.0,true", "2.5,true", "-0.1,false", "5.1,false"})
        void teachingRatingMustBeWithinZeroAndFive(double rating, boolean accepted) {
            assertThat(service.updateTeachingRating("P2", rating)).isEqualTo(accepted);
            assertThat(service.getProfessorById("P2").orElseThrow().getTeachingRating())
                    .isEqualTo(accepted ? rating : 3.0);
        }

        @Test
        void ratingUpdateForUnknownProfessorIsRejected() {
            assertThat(service.updateTeachingRating("NOPE", 4.0)).isFalse();
        }
    }

    @Nested
    class Statistics {

        @Test
        void freshServiceStatisticsArePopulated() {
            // Regression: cache timestamp initialised in the constructor made a fresh
            // service return an empty cached statistics map.
            assertThat(service.calculateOverallStatistics())
                    .containsEntry("totalProfessors", 0)
                    .containsEntry("medianRating", 0.0)
                    .containsEntry("averageExperience", 0.0);
        }

        @Test
        void emptyServiceReportsZeroMinAndMaxRating() {
            // Regression: DoubleSummaryStatistics of no values reported +/-Infinity.
            assertThat(service.calculateOverallStatistics())
                    .containsEntry("minRating", 0.0)
                    .containsEntry("maxRating", 0.0);
        }

        @Test
        void aggregatesReflectProfessors() {
            seedThree();
            add("P4", "Di", "Dijkstra", "MATH", AcademicRank.ASSOCIATE, 3.5, 12, true);
            service.assignCourse("P1", "CS101");
            service.assignCourse("P1", "CS102");

            Map<String, Object> stats = service.calculateOverallStatistics();
            assertThat(stats).containsEntry("totalProfessors", 4)
                    .containsEntry("minRating", 3.0)
                    .containsEntry("maxRating", 4.5)
                    .containsEntry("medianRating", 3.75)
                    .containsEntry("tenuredCount", 2L)
                    .containsEntry("totalCourseAssignments", 2)
                    .containsEntry("averageExperience", 41 / 4.0);
            assertThat((double) stats.get("averageRating")).isCloseTo(15.0 / 4, within(1e-9));
            assertThat(service.getTeachingStatisticsByDepartment()).containsEntry("CS", 2L).containsEntry("MATH", 2L);
            assertThat(service.getProfessorDistributionByRank()).containsEntry(AcademicRank.FULL, 1L).hasSize(4);
            assertThat(service.getAverageRatingByDepartment().get("CS")).isCloseTo(3.75, within(1e-9));
            assertThat(service.getExperienceDistribution())
                    .containsEntry("0-4 years", 1L).containsEntry("5-9 years", 1L)
                    .containsEntry("10-14 years", 1L).containsEntry("20+ years", 1L);
        }

        @Test
        void statisticsAreCachedUntilAMutation() {
            seedThree();
            assertThat(service.calculateOverallStatistics()).containsEntry("totalProfessors", 3);
            assertThat(service.getSummaryStatistics()).containsEntry("totalProfessors", 3);

            add("P4", "Di", "Dijkstra", "MATH", AcademicRank.ASSOCIATE, 3.5, 17, true);
            assertThat(service.calculateOverallStatistics()).containsEntry("totalProfessors", 4);
            assertThat(service.getExperienceDistribution()).containsEntry("15-19 years", 1L);
        }

        @Test
        void oddCountMedian() {
            seedThree();
            assertThat(service.calculateOverallStatistics()).containsEntry("medianRating", 4.0);
        }
    }

    @Nested
    class Searching {

        @BeforeEach
        void seed() {
            seedThree();
        }

        @Test
        void keywordSearchCoversNameDepartmentResearchAndRank() {
            assertThat(ids(service.search("LOVE"))).containsExactly("P1");
            assertThat(ids(service.search("math"))).containsExactly("P3");
            assertThat(ids(service.search("compil"))).containsExactly("P1");
            assertThat(ids(service.search("adjunct"))).containsExactly("P3");
            assertThat(service.countSearchResults("cs")).isEqualTo(2);
        }

        @Test
        void criteriaSearchSupportsAllTextOperators() {
            assertThat(ids(service.search(Map.of("lastName", new SearchCriterion(SearchCriteria.EXACT_MATCH, "codd")))))
                    .containsExactly("P3");
            assertThat(ids(service.search(Map.of("email", new SearchCriterion(SearchCriteria.CONTAINS, "bob@")))))
                    .containsExactly("P2");
            assertThat(ids(service.search(Map.of("professorId", new SearchCriterion(SearchCriteria.ENDS_WITH, "1")))))
                    .containsExactly("P1");
            assertThat(service.search(Map.of("firstName", new SearchCriterion(SearchCriteria.REGEX, ".*")))).isEmpty();
            assertThat(service.search(Map.of("unknown", new SearchCriterion(SearchCriteria.CONTAINS, "x")))).isEmpty();
            assertThat(service.countSearchResults(
                    Map.of("departmentId", new SearchCriterion(SearchCriteria.STARTS_WITH, "c")))).isEqualTo(2);
        }

        @ParameterizedTest
        @CsvSource({
                "firstName,P1;P2;P3", "lastName,P2;P3;P1", "email,P1;P2;P3", "professorId,P1;P2;P3",
                "departmentId,P1;P2;P3", "academicRank,P3;P2;P1", "teachingRating,P2;P3;P1",
                "yearsOfExperience,P3;P2;P1", "whatever,P2;P3;P1"})
        void sortsByEveryField(String field, String expected) {
            List<String> sorted = ids(service.searchAndSort("", field, SortOrder.ASC));
            if (field.equals("departmentId")) {
                // P1 and P2 tie on "CS"; only the relative position of MATH is defined
                assertThat(sorted.get(2)).isEqualTo("P3");
            } else {
                assertThat(sorted).containsExactly(expected.split(";"));
            }
        }

        @Test
        void sortsByTenureDescending() {
            assertThat(ids(service.searchAndSort("", "tenured", SortOrder.DESC)).get(0)).isEqualTo("P1");
        }

        @Test
        void paginationAndAdvancedSearch() {
            SearchResult<Professor> first = service.searchWithPagination("", 0, 2);
            assertThat(first.getResults()).hasSize(2);
            assertThat(first.getTotalElements()).isEqualTo(3);
            assertThat(first.hasNext()).isTrue();

            SearchResult<Professor> advanced = service.advancedSearchWithPagination(
                    Map.of("departmentId", new SearchCriterion(SearchCriteria.EXACT_MATCH, "CS")),
                    "teachingRating", SortOrder.DESC, 0, 1);
            assertThat(ids(advanced.getResults())).containsExactly("P1");
            assertThat(advanced.getTotalElements()).isEqualTo(2);
            assertThat(advanced.getSortBy()).isEqualTo("teachingRating");
            assertThat(advanced.getSortOrder()).isEqualTo(SortOrder.DESC);

            SearchResult<Professor> ascending = service.advancedSearchWithPagination(
                    Map.of(), "teachingRating", SortOrder.ASC, 1, 2);
            assertThat(ids(ascending.getResults())).containsExactly("P1");
        }

        @Test
        void suggestionsAndFieldLists() {
            assertThat(service.getSearchSuggestions("co", 10)).contains("Codd", "Compilers");
            assertThat(service.getSearchSuggestions("o", 2)).hasSize(2);
            assertThat(service.getSearchableFields()).contains("researchArea", "teachingRating");
            assertThat(service.getSortableFields()).contains("tenured", "yearsOfExperience");
        }
    }

    @Nested
    class Reports {

        @BeforeEach
        void seed() {
            seedThree();
            service.assignCourse("P1", "CS101");
            service.addResearchPublications("P1", 4);
        }

        @Test
        void performanceReportHasRowPerProfessor() {
            ReportData report = service.generateReport(ReportType.PERFORMANCE_REPORT);

            assertThat(report.getReportType()).isEqualTo(ReportType.PERFORMANCE_REPORT);
            assertThat(report.getColumns()).contains("Publications");
            assertThat(report.getRows()).hasSize(3)
                    .anySatisfy(row -> assertThat(row)
                            .containsEntry("Professor ID", "P1")
                            .containsEntry("Rating", "4.50")
                            .containsEntry("Experience", "20 years")
                            .containsEntry("Courses", 1)
                            .containsEntry("Publications", 4));
            assertThat(report.getMetadata()).containsEntry("totalProfessors", 3);
        }

        @Test
        void demographicReportCountsDepartmentsAndRanks() {
            ReportData report = service.generateReport(ReportType.DEMOGRAPHIC_REPORT);

            assertThat(report.getRows())
                    .anySatisfy(row -> assertThat(row).containsEntry("Category", "Department")
                            .containsEntry("Value", "CS").containsEntry("Count", 2L)
                            .containsEntry("Percentage", String.format("%.1f%%", 200.0 / 3)))
                    .anySatisfy(row -> assertThat(row).containsEntry("Category", "Academic Rank")
                            .containsEntry("Value", "Full Professor").containsEntry("Count", 1L));
            assertThat(report.getMetadata()).containsEntry("totalProfessors", 3L);
        }

        @Test
        void statisticalSummaryAndUnsupportedTypes() {
            ReportData summary = service.generateReportForDateRange(ReportType.STATISTICAL_SUMMARY,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now());
            assertThat(summary.getContent()).contains("totalProfessors: 3");

            ReportData unsupported = service.generateReport(ReportType.GRADE_REPORT);
            assertThat(unsupported.getTitle()).isEqualTo("Unsupported Report Type");
        }

        @Test
        void reportableStubs() {
            assertThat(service.getAvailableReportTypes()).containsExactly(
                    ReportType.PERFORMANCE_REPORT, ReportType.DEMOGRAPHIC_REPORT, ReportType.STATISTICAL_SUMMARY);
            assertThat(service.getSupportedFormats()).contains(ReportFormat.PDF, ReportFormat.JSON);
            assertThat(service.exportReport(null, ReportFormat.CSV, "ignored")).isTrue();
            assertThat(service.scheduleRecurringReport(ReportType.PERFORMANCE_REPORT, "weekly", List.of()))
                    .startsWith("SCHED_PROF_");
            assertThat(service.cancelScheduledReport("x")).isTrue();
            assertThat(service.getReportHistory(ReportType.PERFORMANCE_REPORT, 5)).isEmpty();
        }
    }
}
