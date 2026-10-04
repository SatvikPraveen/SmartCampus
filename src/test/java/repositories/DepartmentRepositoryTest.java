package repositories;

import models.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentRepositoryTest {

    private DepartmentRepository repo;
    private Department cs;
    private Department math;
    private Department art;
    private Department undated;

    @BeforeEach
    void setUp() {
        repo = new DepartmentRepository();
        cs = department("CS", "Computer Science", "Building A, Floor 2", "PROF000001", "1965", 3);
        math = department("MA", "Mathematics", "Building B", "PROF000010", "1890", 1);
        art = department("AR", "Fine Arts", "Building A, Floor 1", null, "1968", 0);
        undated = department("XX", "Experimental", "Annex", null, "unknown", 2);
    }

    private Department department(String code, String name, String location, String head, String year, int students) {
        Department d = new Department();
        d.setDepartmentCode(code);
        d.setDepartmentName(name);
        d.setLocation(location);
        d.setHeadOfDepartmentId(head);
        d.setEstablishedYear(year);
        for (int i = 0; i < students; i++) d.addStudent(code + "-S" + i, "2024");
        return repo.save(d);
    }

    @Test
    void generatesIds() {
        assertThat(cs.getDepartmentId()).isEqualTo("DEPT001");
        assertThat(undated.getDepartmentId()).isEqualTo("DEPT004");
    }

    @Test
    void nameCodeAndLocationQueries() {
        assertThat(repo.findByDepartmentCode("MA")).containsSame(math);
        assertThat(repo.findByName("computer SCIENCE")).containsSame(cs);
        assertThat(repo.findByNameContaining("ART")).containsExactly(art);
        assertThat(repo.findByBuilding("building a")).containsExactlyInAnyOrder(cs, art);
        assertThat(repo.groupByBuilding()).containsOnlyKeys("Building A", "Building B", "Annex");
        assertThat(repo.groupByBuilding().get("Building A")).containsExactlyInAnyOrder(cs, art);
    }

    @Test
    void findByHeadOfDepartmentMatchesTheExactId() {
        // Regression: a case-insensitive substring match meant PROF00001 also returned PROF000010's department.
        assertThat(repo.findByHeadOfDepartment("PROF000001")).containsExactly(cs);
        assertThat(repo.findByHeadOfDepartment("PROF00001")).isEmpty();
        assertThat(repo.findByHeadOfDepartment("PROF0000")).isEmpty();
        // the *Containing variant is the fuzzy one
        assertThat(repo.findByHeadNameContaining("prof0000")).containsExactlyInAnyOrder(cs, math);
    }

    @Test
    void establishmentYearQueriesIgnoreUnparseableYears() {
        assertThat(repo.findByEstablishmentYear(1965)).containsExactly(cs);
        assertThat(repo.findEstablishedAfter(1965)).containsExactly(art);
        assertThat(repo.findEstablishedBefore(1965)).containsExactly(math);
        assertThat(repo.findByEstablishmentYearRange(1890, 1965)).containsExactlyInAnyOrder(math, cs);
        Map<Integer, List<Department>> byDecade = repo.groupByEstablishmentDecade();
        assertThat(byDecade).containsOnlyKeys(1960, 1890);
        assertThat(byDecade.get(1960)).containsExactlyInAnyOrder(cs, art);
        assertThat(repo.findAllSortedByEstablishmentYear()).containsExactly(math, cs, art, undated);
        assertThat(repo.findOldestDepartments(2)).containsExactly(math, cs);
        assertThat(repo.getAverageEstablishmentYear()).hasValueCloseTo((1965 + 1890 + 1968) / 3.0,
                org.assertj.core.api.Assertions.within(1e-9));
    }

    @Test
    void newestDepartmentsAreRelativeToCurrentYear() {
        Department recent = department("NEW", "New Media", "Annex", null, String.valueOf(Year.now().getValue() - 1), 0);

        assertThat(repo.findNewestDepartments(5)).containsExactly(recent);
    }

    @Test
    void studentCountQueries() {
        assertThat(repo.findByStudentCountRange(1, 2)).containsExactlyInAnyOrder(math, undated);
        assertThat(repo.findByMinStudentCount(2)).containsExactlyInAnyOrder(cs, undated);
        assertThat(repo.findLargeDepartments(2)).containsExactly(cs);
        assertThat(repo.findSmallDepartments(1)).containsExactly(art);
        assertThat(repo.findSimilarSizedDepartments(2, 1)).containsExactlyInAnyOrder(cs, math, undated);
        assertThat(repo.findAllSortedByStudentCount()).extracting(Department::getStudentCount)
                .containsExactly(3, 2, 1, 0);
        assertThat(repo.findAllSortedByName()).extracting(Department::getDepartmentName)
                .containsExactly("Computer Science", "Experimental", "Fine Arts", "Mathematics");
    }

    @Test
    void statistics() {
        assertThat(repo.getDepartmentStatistics())
                .containsEntry("totalDepartments", 4)
                .containsEntry("totalStudentsAcrossAllDepartments", 6)
                .containsEntry("averageStudentCount", 1.5)
                .containsEntry("maxStudentCount", 3)
                .containsEntry("minStudentCount", 0)
                .containsEntry("oldestDepartmentYear", 1890)
                .containsEntry("newestDepartmentYear", 1968);
    }

    @Test
    void searchCombinesCriteria() {
        assertThat(repo.searchDepartments("sci", null, null, null, null, null)).containsExactly(cs);
        assertThat(repo.searchDepartments(null, "prof", "building", null, 1, null)).containsExactlyInAnyOrder(cs, math);
        assertThat(repo.searchDepartments(null, null, null, 1968, null, null)).containsExactly(art);
        assertThat(repo.searchDepartments(null, null, null, null, null, 0)).containsExactly(art);
    }
}
