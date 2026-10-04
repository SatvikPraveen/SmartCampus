package services;

import models.Course;
import models.Course.CourseStatus;
import models.Department;
import models.Enrollment;
import models.Enrollment.EnrollmentStatus;
import models.Grade;
import models.Grade.GradeComponent;
import models.Professor;
import models.Professor.AcademicRank;
import models.Student;
import models.Student.AcademicYear;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import services.SearchService.AdvancedSearchCriteria;
import services.SearchService.AdvancedSearchResult;
import services.SearchService.AutoCompleteResult;
import services.SearchService.FacetedSearchResult;
import services.SearchService.FuzzyMatch;
import services.SearchService.FuzzySearchResult;
import services.SearchService.SearchSuggestion;
import services.SearchService.UniversalSearchResult;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SearchServiceTest {

    private StudentService students;
    private ProfessorService professors;
    private CourseService courses;
    private DepartmentService departments;
    private EnrollmentService enrollments;
    private GradeService grades;
    private SearchService service;

    /**
     * Seeds a small campus: two students, professors, courses and departments
     * (the Mathematics department has no location), two enrollments and two grades.
     */
    @BeforeEach
    void setUp() {
        students = new StudentService();
        professors = new ProfessorService();
        courses = new CourseService();
        departments = new DepartmentService();
        enrollments = new EnrollmentService();
        grades = new GradeService();

        addStudent("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
        addStudent("S2", "Alan", "Turing", "MATH", AcademicYear.SENIOR);
        students.addGrade("S1", graded("GX", "S1", "CS401", "Essay", GradeComponent.PROJECT, 95)); // GPA 4.0

        addProfessor("P1", "Grace", "Hopper", "CS", AcademicRank.FULL, 4.5);
        addProfessor("P2", "Edsger", "Dijkstra", "MATH", AcademicRank.ASSISTANT, 3.0);

        Course compilers = new Course("CS401", "CS401", "Compilers", "About Compilers", 4, "CS");
        compilers.setStatus(CourseStatus.OPEN);
        courses.addCourse(compilers);
        courses.addCourse(new Course("MA101", "MA101", "Calculus", "About Calculus", 3, "MATH"));

        departments.addDepartment(new Department("D1", "CS", "Computer Science", "d", "Building A"));
        departments.addDepartment(new Department("D2", "MATH", "Mathematics", "d", null));

        assertThat(enrollments.enrollStudent("S1", "CS401", "Fall", 2025)).isTrue();
        assertThat(enrollments.enrollStudent("S2", "MA101", "Spring", 2026)).isTrue();

        grades.addGrade(graded("G1", "S1", "CS401", "Lab one", GradeComponent.HOMEWORK, 95));
        grades.addGrade(graded("G2", "S2", "MA101", "Final", GradeComponent.EXAM, 60));

        service = new SearchService(students, professors, courses, departments, enrollments, grades);
    }

    private void addStudent(String id, String first, String last, String major, AcademicYear year) {
        assertThat(students.addStudent(
                new Student("U" + id, first, last, first.toLowerCase() + "@campus.edu", null, id, major, year))).isTrue();
    }

    private void addProfessor(String id, String first, String last, String dept, AcademicRank rank, double rating) {
        Professor p = new Professor("U" + id, first, last, first.toLowerCase() + "@campus.edu", null, id, dept, rank, "x");
        p.setTeachingRating(rating);
        assertThat(professors.addProfessor(p)).isTrue();
    }

    private static Grade graded(String id, String student, String course, String name, GradeComponent c, double points) {
        Grade g = new Grade(id, "E1", student, course, name, c);
        g.setPointsPossible(100);
        g.submitAssignment();
        assertThat(g.gradeAssignment(points, "P1", "")).isTrue();
        return g;
    }

    private AdvancedSearchResult advanced(AdvancedSearchCriteria criteria) {
        return service.advancedSearch(criteria);
    }

    private static List<String> studentIds(List<?> results) {
        return results.stream().map(o -> ((Student) o).getStudentId()).toList();
    }

    @Nested
    class Universal {

        @Test
        void searchesEveryEntityType() {
            UniversalSearchResult result = service.searchAll("cs");

            assertThat(result.getQuery()).isEqualTo("cs");
            assertThat(result.getResults()).containsOnlyKeys("students", "professors", "courses", "departments",
                    "enrollments", "grades");
            assertThat(result.getResultsForType("students", Student.class))
                    .extracting(Student::getStudentId).containsExactly("S1");
            assertThat(result.getResultsForType("professors", Professor.class))
                    .extracting(Professor::getProfessorId).containsExactly("P1");
            assertThat(result.getResultsForType("courses", Course.class))
                    .extracting(Course::getCourseId).containsExactly("CS401");
            assertThat(result.getResultsForType("departments", Department.class))
                    .extracting(Department::getDepartmentId).containsExactlyInAnyOrder("D1", "D2"); // "mathematiCS"
            assertThat(result.getResultsForType("enrollments", Enrollment.class))
                    .extracting(Enrollment::getStudentId).containsExactly("S1");
            assertThat(result.getResultsForType("grades", Grade.class))
                    .extracting(Grade::getGradeId).containsExactly("G1");
            assertThat(result.getTotalResults()).isEqualTo(7);
            assertThat(result.hasResults()).isTrue();
            assertThat(result.getResultsForType("students", Professor.class)).isEmpty();
            assertThat(result.getResultsForType("nothing", Student.class)).isEmpty();
            assertThat(result.toString()).contains("query='cs'", "totalResults=7");
        }

        @Test
        void blankQueryReturnsNothing() {
            assertThat(service.searchAll(" ").hasResults()).isFalse();
            assertThat(service.searchAll(null).getResults()).isEmpty();
        }

        @Test
        void aFailingServiceIsReportedAsAnErrorEntry() {
            CourseService failing = new CourseService() {
                @Override
                public List<Course> search(String keyword) {
                    throw new IllegalStateException("course index unavailable");
                }
            };
            SearchService withFailure = new SearchService(students, professors, failing, departments, enrollments, grades);

            UniversalSearchResult result = withFailure.searchAll("cs");

            assertThat(result.getResults()).containsKey("error");
            assertThat(result.getResults().get("error").get(0).toString())
                    .startsWith("Search operation failed").contains("course index unavailable");
        }

        @Test
        void indexMaintenanceDoesNotAffectResults() {
            service.clearSearchCache();
            service.rebuildSearchIndex();

            assertThat(service.searchAll("lovelace").getTotalResults()).isEqualTo(1);
        }
    }

    @Nested
    class Advanced {

        @Test
        void studentCriteria() {
            assertThat(studentIds(advanced(new AdvancedSearchCriteria().withMajor("CS")).getResults().get("students")))
                    .containsExactly("S1");
            assertThat(studentIds(advanced(new AdvancedSearchCriteria().withAcademicYear(AcademicYear.SENIOR))
                    .getResults().get("students"))).containsExactly("S2");
            assertThat(studentIds(advanced(new AdvancedSearchCriteria().withMinGpa(3.5))
                    .getResults().get("students"))).containsExactly("S1");
        }

        @Test
        void professorAndCourseCriteria() {
            AdvancedSearchResult math = advanced(new AdvancedSearchCriteria().withDepartmentId("MATH"));
            assertThat(math.getResults().get("professors")).extracting(p -> ((Professor) p).getProfessorId())
                    .containsExactly("P2");
            assertThat(math.getResults().get("courses")).extracting(c -> ((Course) c).getCourseId())
                    .containsExactly("MA101");

            assertThat(advanced(new AdvancedSearchCriteria().withAcademicRank(AcademicRank.FULL)).getResults()
                    .get("professors")).extracting(p -> ((Professor) p).getProfessorId()).containsExactly("P1");
            assertThat(advanced(new AdvancedSearchCriteria().withMinTeachingRating(4.0)).getResults()
                    .get("professors")).extracting(p -> ((Professor) p).getProfessorId()).containsExactly("P1");
            assertThat(advanced(new AdvancedSearchCriteria().withCourseStatus(CourseStatus.OPEN)).getResults()
                    .get("courses")).extracting(c -> ((Course) c).getCourseId()).containsExactly("CS401");
            assertThat(advanced(new AdvancedSearchCriteria().withMinCreditHours(4)).getResults()
                    .get("courses")).extracting(c -> ((Course) c).getCourseId()).containsExactly("CS401");
        }

        @Test
        void departmentEnrollmentAndGradeCriteria() {
            assertThat(advanced(new AdvancedSearchCriteria().withLocation("Building A")).getResults()
                    .get("departments")).extracting(d -> ((Department) d).getDepartmentId()).containsExactly("D1");

            assertThat(advanced(new AdvancedSearchCriteria().withSemester("Fall")).getResults().get("enrollments"))
                    .extracting(e -> ((Enrollment) e).getStudentId()).containsExactly("S1");
            assertThat(advanced(new AdvancedSearchCriteria().withYear(2026)).getResults().get("enrollments"))
                    .extracting(e -> ((Enrollment) e).getStudentId()).containsExactly("S2");
            assertThat(advanced(new AdvancedSearchCriteria().withEnrollmentStatus(EnrollmentStatus.DROPPED))
                    .getResults().get("enrollments")).isEmpty();
            assertThat(advanced(new AdvancedSearchCriteria().withEnrollmentStatus(EnrollmentStatus.ENROLLED))
                    .getResults().get("enrollments")).hasSize(2);

            assertThat(advanced(new AdvancedSearchCriteria().withGradeComponent(GradeComponent.EXAM)).getResults()
                    .get("grades")).extracting(g -> ((Grade) g).getGradeId()).containsExactly("G2");
            assertThat(advanced(new AdvancedSearchCriteria().withMinGradePercentage(90.0)).getResults()
                    .get("grades")).extracting(g -> ((Grade) g).getGradeId()).containsExactly("G1");
        }

        @Test
        void textQueryAndRelevance() {
            AdvancedSearchResult result = advanced(new AdvancedSearchCriteria("lovelace"));

            assertThat(studentIds(result.getResults().get("students"))).containsExactly("S1");
            assertThat(result.getTotalResults()).isEqualTo(1);
            // 1 result -> 0.1 base, plus 8-char query / 50 specificity
            assertThat(result.getRelevanceScore("students")).isCloseTo(0.1 + 8 / 50.0, within(1e-9));
            assertThat(result.getRelevanceScore("courses")).isZero();
            assertThat(result.getRelevanceScore("missing")).isZero();
            assertThat(result.getCriteria().getQuery()).isEqualTo("lovelace");
            assertThat(result.toString()).contains("totalResults=1");

            AdvancedSearchResult noQuery = advanced(new AdvancedSearchCriteria().withMajor("CS"));
            assertThat(noQuery.getRelevanceScores().get("students")).isCloseTo(0.1, within(1e-9));
        }

        @Test
        void exclusionsAndLimits() {
            AdvancedSearchCriteria criteria = new AdvancedSearchCriteria().withQuery("a")
                    .excludeProfessors().excludeCourses().excludeDepartments().excludeEnrollments().excludeGrades()
                    .withMaxResults(1);

            AdvancedSearchResult result = advanced(criteria);
            assertThat(result.getResults()).containsOnlyKeys("students");
            assertThat(result.getResults().get("students")).hasSize(1);
            assertThat(criteria.toString()).contains("maxResults=1", "Students,").doesNotContain("Grades");

            assertThat(advanced(new AdvancedSearchCriteria().excludeStudents()).getResults())
                    .doesNotContainKey("students");
            assertThat(new AdvancedSearchCriteria().toString()).contains("Students,Professors,Courses,Departments,Enrollments,Grades");
        }
    }

    @Nested
    class Fuzzy {

        @Test
        void toleratesTyposInIndividualWords() {
            // Regression: similarity was measured against the whole concatenated search text
            // (name + email + id + ...), so even an exact surname scored far below any threshold.
            FuzzySearchResult result = service.fuzzySearch("Lovelase", 0.8);

            assertThat(result.getResults().get("students")).singleElement().satisfies(match -> {
                assertThat(((Student) match.getEntity()).getStudentId()).isEqualTo("S1");
                assertThat(match.getScore()).isCloseTo(1 - 1 / 8.0, within(1e-9));
            });
            assertThat(result.getResults().get("professors")).isEmpty();
            assertThat(result.getTotalMatches()).isEqualTo(1);
            assertThat(result.getQuery()).isEqualTo("Lovelase");
            assertThat(result.getThreshold()).isEqualTo(0.8);
        }

        @Test
        void matchesEachEntityTypeAndRanksByScore() {
            assertThat(service.fuzzySearch("Hoper", 0.8).getResults().get("professors")).hasSize(1);
            assertThat(service.fuzzySearch("Compiler", 0.8).getResults().get("courses")).hasSize(1);
            assertThat(service.fuzzySearch("Mathematic", 0.8).getResults().get("departments")).hasSize(1);

            FuzzySearchResult loose = service.fuzzySearch("Alan", 0.5);
            List<FuzzyMatch<?>> matches = loose.getResults().get("students");
            assertThat(matches).isSortedAccordingTo(Comparator.comparingDouble(FuzzyMatch<?>::getScore).reversed());
            assertThat(((Student) matches.get(0).getEntity()).getStudentId()).isEqualTo("S2");
            assertThat(loose.getAverageScore()).isBetween(0.5, 1.0);
            assertThat(loose.toString()).contains("query='Alan'");
            assertThat(matches.get(0).toString()).contains("score=1.000");
        }

        @Test
        void invalidInputsYieldNoMatches() {
            assertThat(service.fuzzySearch("", 0.5).getTotalMatches()).isZero();
            assertThat(service.fuzzySearch("ada", -0.1).getResults()).isEmpty();
            assertThat(service.fuzzySearch("ada", 1.1).getAverageScore()).isZero();
        }
    }

    @Nested
    class Faceted {

        @Test
        void filtersByFacetAndCountsUnfilteredValues() {
            FacetedSearchResult result = service.facetedSearch("a", Map.of("students", Set.of("MATH")));

            assertThat(studentIds(result.getResults().get("students"))).containsExactly("S2");
            assertThat(result.getFacetCountsForType("students")).containsEntry("CS", 1L).containsEntry("MATH", 1L);
            assertThat(result.getFacetCountsForType("missing")).isEmpty();
            assertThat(result.getAppliedFacets()).containsOnlyKeys("students");
            assertThat(result.getQuery()).isEqualTo("a");
            assertThat(result.getFacetCounts()).containsKey("professors");
            assertThat(result.toString()).contains("query='a'");
        }

        @Test
        void departmentWithoutLocationDoesNotBreakFacetCounts() {
            // Regression: a null facet value (department without a location) made
            // Collectors.groupingBy throw a NullPointerException.
            FacetedSearchResult result = service.facetedSearch("math", Map.of());

            assertThat(result.getFacetCountsForType("departments")).containsExactly(Map.entry("unknown", 1L));
            assertThat(result.getFacetCountsForType("professors")).containsEntry("MATH", 1L);
            assertThat(result.getFacetCountsForType("courses")).containsEntry("MATH", 1L);
            assertThat(result.getFacetCountsForType("students")).containsEntry("MATH", 1L);
            assertThat(result.getTotalResults()).isPositive();
        }

        @Test
        void departmentFacetFiltersByLocation() {
            FacetedSearchResult result = service.facetedSearch("c", Map.of("departments", Set.of("Building A")));

            assertThat(result.getResults().get("departments")).extracting(d -> ((Department) d).getDepartmentId())
                    .containsExactly("D1");
        }
    }

    @Nested
    class Suggestions {

        @Test
        void smallLimitsStillProduceSuggestions() {
            // Regression: the limit was split as maxSuggestions / 6 per service, which is 0
            // for any limit below 6, so no suggestions were ever returned.
            List<SearchSuggestion> suggestions = service.getSearchSuggestions("ada", 3);

            assertThat(suggestions).extracting(SearchSuggestion::getSuggestion).contains("Ada");
            assertThat(suggestions).hasSizeLessThanOrEqualTo(3);
        }

        @Test
        void prefixMatchesOutrankContainsMatches() {
            List<SearchSuggestion> suggestions = service.getSearchSuggestions("lo", 10);

            assertThat(suggestions).isSortedAccordingTo(Comparator.comparingDouble(SearchSuggestion::getScore).reversed());
            SearchSuggestion lovelace = suggestions.stream().filter(s -> s.getSuggestion().equals("Lovelace"))
                    .findFirst().orElseThrow();
            assertThat(lovelace.getScore()).isEqualTo(1.0);
            assertThat(lovelace.getEntityType()).isEqualTo("students");
            assertThat(service.getSearchSuggestions("velace", 10)).singleElement()
                    .satisfies(s -> assertThat(s.getScore()).isEqualTo(0.7));
        }

        @Test
        void blankInputHasNoSuggestions() {
            assertThat(service.getSearchSuggestions(" ", 5)).isEmpty();
            assertThat(service.getSearchSuggestions(null, 5)).isEmpty();
        }

        @Test
        void suggestionEqualityIgnoresScore() {
            SearchSuggestion a = new SearchSuggestion("Ada", "students", 1.0);
            SearchSuggestion b = new SearchSuggestion("Ada", "students", 0.2);

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(new SearchSuggestion("Ada", "professors", 1.0));
            assertThat(a).isEqualTo(a).isNotEqualTo(null).isNotEqualTo("Ada");
            assertThat(a.toString()).contains("Ada", "students");
        }
    }

    @Nested
    class AutoComplete {

        @Test
        void shortQueriesOnlySuggest() {
            AutoCompleteResult result = service.searchWithAutoComplete("ad", true);

            assertThat(result.getQuery()).isEqualTo("ad");
            assertThat(result.isIncludePartial()).isTrue();
            assertThat(result.hasSuggestions()).isTrue();
            assertThat(result.getSearchResults()).isNull();
            assertThat(result.hasSearchResults()).isFalse();
        }

        @Test
        void longerQueriesAlsoSearch() {
            AutoCompleteResult result = service.searchWithAutoComplete("ada", false);

            assertThat(result.hasSearchResults()).isTrue();
            assertThat(result.getSearchResults().getTotalResults()).isEqualTo(1);
            assertThat(result.getSuggestions()).isNotEmpty();
            assertThat(result.toString()).contains("hasResults=true");
        }
    }
}
