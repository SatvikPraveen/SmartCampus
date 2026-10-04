package repositories;

import models.Department;
import models.Professor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessorRepositoryTest {

    private ProfessorRepository repo;
    private Professor ada;
    private Professor alan;
    private Professor grace;

    @BeforeEach
    void setUp() {
        repo = new ProfessorRepository();
        ada = professor("Ada", "Lovelace", "ada@uni.edu", "D-CS", "Algorithms, Analysis", "Turing Hall 301", 25);
        alan = professor("Alan", "Turing", "alan@uni.edu", "D-CS", "Computability", "Turing Hall 105", 5);
        grace = professor("Grace", "Hopper", "grace@navy.mil", "D-EE", "Compilers; Languages", "Watt Bldg 3rd floor", 12);
    }

    private Professor professor(String first, String last, String email, String dept, String spec,
                                String office, int years) {
        Professor p = new Professor("U-" + first, first, last, email, null, null, dept,
                Professor.AcademicRank.ASSOCIATE, spec);
        p.setOfficeLocation(office);
        p.setYearsOfExperience(years);
        return repo.save(p);
    }

    @Test
    void generatesIds() {
        assertThat(ada.getProfessorId()).isEqualTo("PROF000001");
        assertThat(grace.getProfessorId()).isEqualTo("PROF000003");
    }

    @Test
    void attributeQueries() {
        Department cs = new Department("D-CS", "CS", "Computer Science", "", "A");

        assertThat(repo.findByDepartment(cs)).containsExactlyInAnyOrder(ada, alan);
        assertThat(repo.findByEmail("grace@navy.mil")).containsSame(grace);
        assertThat(repo.findByEmailDomain("UNI.EDU")).containsExactlyInAnyOrder(ada, alan);
        assertThat(repo.findBySpecialization("COMP")).containsExactlyInAnyOrder(alan, grace);
        assertThat(repo.findByNameContaining("tur")).containsExactly(alan);
        assertThat(repo.findByOfficeBuilding("turing hall")).containsExactlyInAnyOrder(ada, alan);
        assertThat(repo.findByOfficeLocation("301")).containsExactly(ada);
        assertThat(repo.findByOfficeFloor("3RD")).containsExactly(grace);
        assertThat(repo.findProfessorsWithMultipleSpecializations()).containsExactlyInAnyOrder(ada, grace);
    }

    @ParameterizedTest(name = "threshold {0}")
    @CsvSource({"5, 2, 1", "12, 1, 2", "25, 0, 3", "0, 3, 0"})
    void seniorAndJuniorPartitionAllProfessors(int threshold, int senior, int junior) {
        assertThat(repo.findSeniorProfessors(threshold)).hasSize(senior);
        assertThat(repo.findJuniorProfessors(threshold)).hasSize(junior);
        assertThat(senior + junior).isEqualTo(3);
    }

    @Test
    void experienceRanges() {
        assertThat(repo.findByMinExperience(12)).containsExactlyInAnyOrder(ada, grace);
        assertThat(repo.findByExperienceRange(5, 12)).containsExactlyInAnyOrder(alan, grace);
        assertThat(repo.findByExperienceRange(13, 24)).isEmpty();
    }

    @Test
    void aggregates() {
        assertThat(repo.getProfessorCountByDepartment()).isEqualTo(Map.of("D-CS", 2L, "D-EE", 1L));
        assertThat(repo.getAverageExperienceByDepartment()).containsEntry("D-CS", 15.0).containsEntry("D-EE", 12.0);
        assertThat(repo.groupBySpecialization()).hasSize(3);
        assertThat(repo.getExperienceStatistics())
                .containsEntry("totalProfessors", 3)
                .containsEntry("averageExperience", 14.0)
                .containsEntry("maxExperience", 25)
                .containsEntry("minExperience", 5);
        assertThat(new ProfessorRepository().getExperienceStatistics())
                .containsEntry("totalProfessors", 0)
                .containsEntry("averageExperience", 0.0);
    }

    @Test
    void searchCombinesCriteria() {
        Department cs = new Department("D-CS", "CS", "Computer Science", "", "A");

        assertThat(repo.searchProfessors("a", cs, null, 10, null)).containsExactly(ada);
        assertThat(repo.searchProfessors(null, null, "comp", null, 12)).containsExactlyInAnyOrder(alan, grace);
        assertThat(repo.searchProfessors(" ", null, "", null, null)).hasSize(3);
    }
}
