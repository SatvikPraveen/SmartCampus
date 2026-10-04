package enums;

import enums.UserRole.Permission;
import enums.UserRole.RoleCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleTest {

    @Test
    void hierarchyComparisons() {
        assertThat(UserRole.DEAN.isHigherThan(UserRole.DEPARTMENT_CHAIR)).isTrue();
        assertThat(UserRole.STUDENT.isLowerThan(UserRole.TEACHING_ASSISTANT)).isTrue();
        assertThat(UserRole.REGISTRAR.isEqualTo(UserRole.ASSISTANT_PROFESSOR)).isTrue();
        assertThat(UserRole.getMinHierarchyLevel()).isEqualTo(1);
        assertThat(UserRole.getMaxHierarchyLevel()).isEqualTo(12);
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void canManageIsStrictAndAntisymmetric(UserRole role) {
        assertThat(role.canManage(role)).isFalse();
        for (UserRole other : UserRole.values()) {
            assertThat(role.canManage(other) && other.canManage(role)).isFalse();
            assertThat(role.canManage(other)).isEqualTo(role.isHigherThan(other));
        }
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void subordinatesAndSuperiorsAreDisjointAndExcludeSelf(UserRole role) {
        List<UserRole> subordinates = role.getSubordinateRoles();
        List<UserRole> superiors = role.getSuperiorRoles();
        assertThat(subordinates).doesNotContain(role).noneMatch(superiors::contains);
        assertThat(superiors).doesNotContain(role).allMatch(r -> r.isHigherThan(role));
        assertThat(subordinates).allMatch(r -> !r.isHigherThan(role));
        assertThat(subordinates.size() + superiors.size()).isEqualTo(UserRole.values().length - 1);
    }

    @Test
    void superAdminHasEveryPermissionAndNoSuperiors() {
        assertThat(UserRole.SUPER_ADMIN.getPermissions()).containsExactlyInAnyOrder(Permission.values());
        assertThat(UserRole.SUPER_ADMIN.getSuperiorRoles()).isEmpty();
        assertThat(UserRole.STUDENT.getSubordinateRoles()).isEmpty();
    }

    @Test
    void onlyStudentRolesCanEnrollInCourses() {
        for (UserRole role : UserRole.values()) {
            if (role == UserRole.SUPER_ADMIN) {
                continue;
            }
            assertThat(role.hasPermission(Permission.ENROLL_IN_COURSES))
                    .as("%s enroll permission", role)
                    .isEqualTo(role.isStudentRole());
        }
    }

    @Test
    void getPermissionsReturnsADefensiveCopy() {
        List<Permission> permissions = UserRole.STUDENT.getPermissions();
        permissions.add(Permission.FULL_SYSTEM_ACCESS);
        assertThat(UserRole.STUDENT.hasPermission(Permission.FULL_SYSTEM_ACCESS)).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "STUDENT, STUDENT", "GRADUATE_STUDENT, STUDENT", "FULL_PROFESSOR, FACULTY",
            "TEACHING_ASSISTANT, FACULTY", "REGISTRAR, STAFF", "IT_SUPPORT, STAFF",
            "DEAN, ADMINISTRATIVE", "SUPER_ADMIN, ADMINISTRATIVE"
    })
    void category(UserRole role, RoleCategory expected) {
        assertThat(role.getCategory()).isEqualTo(expected);
    }

    @Test
    void roleGroupsRelateAsDocumented() {
        assertThat(UserRole.getStudentRoles()).doesNotContainAnyElementsOf(UserRole.getFacultyRoles());
        assertThat(UserRole.getAdministrativeRoles()).containsAll(UserRole.getStaffRoles());
        assertThat(UserRole.getFacultyRoles()).doesNotContainAnyElementsOf(UserRole.getAdministrativeRoles());
    }

    @Test
    void lookupHelpers() {
        assertThat(UserRole.findByDisplayName("teaching assistant")).contains(UserRole.TEACHING_ASSISTANT);
        assertThat(UserRole.findByDisplayName("janitor")).isEmpty();
        assertThat(UserRole.getRolesByLevel(5))
                .containsExactlyInAnyOrder(UserRole.ADJUNCT_PROFESSOR, UserRole.ACADEMIC_ADVISOR,
                        UserRole.FINANCIAL_AID_OFFICER);
        assertThat(UserRole.getRolesByLevel(42)).isEmpty();
    }

    @Test
    void commonAndUniquePermissions() {
        assertThat(UserRole.STUDENT_LEADER.getCommonPermissions(UserRole.STUDENT))
                .containsExactlyInAnyOrderElementsOf(UserRole.STUDENT.getPermissions());
        assertThat(UserRole.STUDENT_LEADER.getUniquePermissions(UserRole.STUDENT))
                .containsExactlyInAnyOrder(Permission.ORGANIZE_STUDENT_EVENTS, Permission.ACCESS_STUDENT_RESOURCES);
        assertThat(UserRole.STUDENT.getUniquePermissions(UserRole.SUPER_ADMIN)).isEmpty();
    }

    @Test
    void stringRepresentations() {
        assertThat(UserRole.DEAN.toString()).isEqualTo("Dean (Level 10): Head of academic college");
        assertThat(Permission.HIRE_FACULTY.toString()).isEqualTo("Hire faculty");
    }
}
