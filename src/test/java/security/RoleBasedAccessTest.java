package security;

import enums.UserRole;
import models.Admin;
import models.Professor;
import models.Student;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static enums.UserRole.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static security.RoleBasedAccess.*;

class RoleBasedAccessTest {

    private final RoleBasedAccess rba = new RoleBasedAccess();

    @Nested
    class Permissions {

        @ParameterizedTest(name = "{0} {1}:{2} -> {3}")
        @CsvSource({
                "STUDENT,              course_management,     read,     true",
                "STUDENT,              course_management,     create,   false",
                "STUDENT,              enrollment_management, create,   true",
                "STUDENT,              grade_management,      update,   false",
                "TEACHING_ASSISTANT,   grade_management,      read,     true",
                "ADJUNCT_PROFESSOR,    course_management,     create,   true",
                "ADJUNCT_PROFESSOR,    course_management,     delete,   false",
                "FULL_PROFESSOR,       grade_management,      update,   true",
                "DEAN,                 student_management,    delete,   false",
                "SYSTEM_ADMINISTRATOR, course_management,     delete,   true",
                "SYSTEM_ADMINISTRATOR, system_administration, read,     false",
                "SYSTEM_ADMINISTRATOR, user_management,       reset_password, false",
                "SUPER_ADMIN,          system_administration, execute,  true",
                "SUPER_ADMIN,          user_management,       reset_password, true",
                "SUPER_ADMIN,          student_management,    delete,   true",
                "SUPER_ADMIN,          grade_management,      create,   true",
                "REGISTRAR,            course_management,     read,     false",
        })
        void permissionMatrix(UserRole role, String resource, String action, boolean expected) {
            assertThat(rba.hasPermission(role, resource, action)).isEqualTo(expected);
        }

        @Test
        void higherTiersInheritLowerTierPermissions() {
            assertThat(rba.hasPermission(FULL_PROFESSOR, ENROLLMENT_MANAGEMENT, CREATE)).isTrue();
            assertThat(rba.hasPermission(SYSTEM_ADMINISTRATOR, "professor_profile", UPDATE)).isTrue();
            assertThat(rba.getAllPermissions(SUPER_ADMIN))
                    .containsAll(rba.getAllPermissions(SYSTEM_ADMINISTRATOR))
                    .containsAll(rba.getAllPermissions(DEAN));
            assertThat(rba.getAllPermissions(DEAN)).containsAll(rba.getAllPermissions(STUDENT));
        }

        @Test
        void nullArgumentsNeverGrantAccess() {
            assertThat(rba.hasPermission(null, COURSE_MANAGEMENT, READ)).isFalse();
            assertThat(rba.hasPermission(STUDENT, null, READ)).isFalse();
            assertThat(rba.hasPermission(STUDENT, COURSE_MANAGEMENT, null)).isFalse();
            assertThat(rba.hasAnyPermission(null, COURSE_MANAGEMENT)).isFalse();
            assertThat(rba.getAllPermissions(null)).isEmpty();
        }

        @Test
        void hasAnyPermissionMatchesWholeResourceNameOnly() {
            assertThat(rba.hasAnyPermission(STUDENT, COURSE_MANAGEMENT)).isTrue();
            assertThat(rba.hasAnyPermission(STUDENT, "course")).isFalse();
            assertThat(rba.hasAnyPermission(STUDENT, SYSTEM_ADMINISTRATION)).isFalse();
        }

        @Test
        void resourcePermissionsListsActions() {
            assertThat(rba.getResourcePermissions(STUDENT, ENROLLMENT_MANAGEMENT))
                    .containsExactlyInAnyOrder(CREATE, READ, DELETE);
            assertThat(rba.getResourcePermissions(SUPER_ADMIN, SYSTEM_ADMINISTRATION))
                    .containsExactlyInAnyOrder(CREATE, READ, UPDATE, DELETE, EXECUTE);
        }

        @Test
        void addAndRemovePermissionAffectOnlyThatRoleAndInstance() {
            rba.addPermission(STUDENT, REPORTS, EXPORT);
            assertThat(rba.hasPermission(STUDENT, REPORTS, EXPORT)).isTrue();
            assertThat(rba.hasPermission(STUDENT_LEADER, REPORTS, EXPORT)).isFalse();
            assertThat(new RoleBasedAccess().hasPermission(STUDENT, REPORTS, EXPORT)).isFalse();

            rba.removePermission(STUDENT, COURSE_MANAGEMENT, READ);
            assertThat(rba.hasPermission(STUDENT, COURSE_MANAGEMENT, READ)).isFalse();
            assertThat(rba.hasPermission(GRADUATE_STUDENT, COURSE_MANAGEMENT, READ)).isTrue();
        }

        @Test
        void addPermissionToRoleWithoutPermissions() {
            rba.addPermission(REGISTRAR, ENROLLMENT_MANAGEMENT, APPROVE);
            assertThat(rba.hasPermission(REGISTRAR, ENROLLMENT_MANAGEMENT, APPROVE)).isTrue();
        }

        @Test
        void rolesWithPermission() {
            assertThat(rba.getRolesWithPermission(SYSTEM_ADMINISTRATION, EXECUTE)).containsExactly(SUPER_ADMIN);
            assertThat(rba.getRolesWithPermission(COURSE_MANAGEMENT, DELETE))
                    .containsExactlyInAnyOrder(SYSTEM_ADMINISTRATOR, SUPER_ADMIN);
        }

        @Test
        void requiredRoleAndPermissions() {
            assertThat(rba.getRequiredRole(SYSTEM_ADMINISTRATION, EXECUTE)).isEqualTo(SUPER_ADMIN);
            assertThat(rba.getRequiredRole(COURSE_MANAGEMENT, READ)).isEqualTo(ADJUNCT_PROFESSOR);
            assertThat(rba.getRequiredRole(COURSE_MANAGEMENT, EXPORT)).isNull();
            assertThat(rba.getRequiredRole("unknown", READ)).isNull();
            assertThat(rba.getRequiredPermissions(REPORTS, READ)).containsExactly("reports:read");
        }

        @Test
        void matrixAndReportAreConsistent() {
            Map<UserRole, Map<String, java.util.Set<String>>> matrix = rba.getPermissionMatrix();

            assertThat(matrix).containsOnlyKeys(UserRole.values());
            assertThat(matrix.get(REGISTRAR)).isEmpty();
            RoleBasedAccess.SecurityReport report = rba.getRoleSecurityReport(STUDENT);
            assertThat(report.getPermissionsByResource()).isEqualTo(matrix.get(STUDENT));
            assertThat(report.getTotalPermissions()).isEqualTo(rba.getAllPermissions(STUDENT).size());
            assertThat(report.getInheritedRoles()).isEmpty();
            assertThat(rba.getRoleSecurityReport(SYSTEM_ADMINISTRATOR).getInheritedRoles())
                    .contains(STUDENT, DEAN).doesNotContain(SUPER_ADMIN, SYSTEM_ADMINISTRATOR);
        }
    }

    @Nested
    class Hierarchy {

        @ParameterizedTest(name = "{0} hasRole {1} -> {2}")
        @CsvSource({
                "STUDENT,              STUDENT,              true",
                "STUDENT,              TEACHING_ASSISTANT,   false",
                "DEAN,                 STUDENT,              true",
                "DEAN,                 FULL_PROFESSOR,       false",
                "STUDENT,              DEAN,                 false",
                "SYSTEM_ADMINISTRATOR, DEAN,                 true",
                "SYSTEM_ADMINISTRATOR, SUPER_ADMIN,          false",
                "SUPER_ADMIN,          SYSTEM_ADMINISTRATOR, true",
                "REGISTRAR,            STUDENT,              false",
        })
        void hasRole(UserRole user, UserRole required, boolean expected) {
            assertThat(rba.hasRole(user, required)).isEqualTo(expected);
        }

        @Test
        void hasRoleRejectsNulls() {
            assertThat(rba.hasRole(null, STUDENT)).isFalse();
            assertThat(rba.hasRole(STUDENT, null)).isFalse();
        }

        @Test
        void canActOnBehalfRequiresPermissionAndSeniority() {
            assertThat(rba.canActOnBehalf(SYSTEM_ADMINISTRATOR, STUDENT, STUDENT_MANAGEMENT, UPDATE)).isTrue();
            assertThat(rba.canActOnBehalf(SYSTEM_ADMINISTRATOR, SUPER_ADMIN, STUDENT_MANAGEMENT, UPDATE)).isFalse();
            assertThat(rba.canActOnBehalf(SYSTEM_ADMINISTRATOR, SYSTEM_ADMINISTRATOR, STUDENT_MANAGEMENT, UPDATE)).isFalse();
            assertThat(rba.canActOnBehalf(FULL_PROFESSOR, STUDENT, STUDENT_MANAGEMENT, UPDATE)).isFalse();
            assertThat(rba.canActOnBehalf(STUDENT, STUDENT, ENROLLMENT_MANAGEMENT, CREATE)).isFalse();
        }

        @Test
        void addRoleHierarchyToRoleWithNoSubordinates() {
            // Regression: student-tier roles were mapped to an immutable Set.of(), so this threw.
            assertThatCode(() -> rba.addRoleHierarchy(STUDENT_LEADER, STUDENT)).doesNotThrowAnyException();

            assertThat(rba.hasRole(STUDENT_LEADER, STUDENT)).isTrue();
            assertThat(rba.hasPermission(STUDENT_LEADER, COURSE_MANAGEMENT, READ)).isTrue();
        }

        @Test
        void hierarchyChangesAreScopedToOneRoleAndOneInstance() {
            // Regression: all professor-tier roles shared one (static) subordinate set, so editing one
            // role's subordinates silently edited every professor role in every RoleBasedAccess.
            rba.addRoleHierarchy(ADJUNCT_PROFESSOR, REGISTRAR);
            rba.removeRoleHierarchy(DEAN, STUDENT);

            assertThat(rba.hasRole(ADJUNCT_PROFESSOR, REGISTRAR)).isTrue();
            assertThat(rba.hasRole(FULL_PROFESSOR, REGISTRAR)).isFalse();
            assertThat(rba.hasRole(DEAN, STUDENT)).isFalse();
            assertThat(rba.hasRole(ADJUNCT_PROFESSOR, STUDENT)).isTrue();

            RoleBasedAccess fresh = new RoleBasedAccess();
            assertThat(fresh.hasRole(ADJUNCT_PROFESSOR, REGISTRAR)).isFalse();
            assertThat(fresh.hasRole(DEAN, STUDENT)).isTrue();
        }

        @Test
        void removeRoleHierarchyForUnknownParentIsNoOp() {
            assertThatCode(() -> rba.removeRoleHierarchy(REGISTRAR, STUDENT)).doesNotThrowAnyException();
        }
    }

    @Nested
    class Context {

        @Test
        void studentsMayOnlySeeTheirOwnGrades() {
            Map<String, Object> own = Map.of("requestingUserId", "s1", "targetUserId", "s1");
            Map<String, Object> other = Map.of("requestingUserId", "s1", "targetUserId", "s2");

            assertThat(rba.hasPermissionWithContext(STUDENT, GRADE_MANAGEMENT, READ, own)).isTrue();
            assertThat(rba.hasPermissionWithContext(STUDENT, GRADE_MANAGEMENT, READ, other)).isFalse();
            assertThat(rba.hasPermissionWithContext(STUDENT, "profile", UPDATE, other)).isFalse();
            assertThat(rba.hasPermissionWithContext(STUDENT, GRADE_MANAGEMENT, READ, Map.of())).isTrue();
            assertThat(rba.hasPermissionWithContext(STUDENT, GRADE_MANAGEMENT, READ, null)).isTrue();
        }

        @Test
        void contextNeverGrantsMissingBasePermission() {
            Map<String, Object> own = Map.of("requestingUserId", "s1", "targetUserId", "s1");

            assertThat(rba.hasPermissionWithContext(STUDENT, GRADE_MANAGEMENT, UPDATE, own)).isFalse();
            assertThat(rba.canAccessEntity(STUDENT, GRADE_MANAGEMENT, "g1", "s1", UPDATE)).isFalse();
            assertThat(rba.canAccessEntity(FULL_PROFESSOR, GRADE_MANAGEMENT, "g1", "p1", UPDATE)).isTrue();
        }

        @Test
        void adminCannotPromoteToSuperAdminEvenWithPermission() {
            rba.addPermission(SYSTEM_ADMINISTRATOR, USER_MANAGEMENT, "change_role");

            assertThat(rba.hasPermissionWithContext(SYSTEM_ADMINISTRATOR, USER_MANAGEMENT, "change_role",
                    Map.of("targetRole", SUPER_ADMIN))).isFalse();
            assertThat(rba.hasPermissionWithContext(SYSTEM_ADMINISTRATOR, USER_MANAGEMENT, "change_role",
                    Map.of("targetRole", FULL_PROFESSOR))).isTrue();
            assertThat(rba.hasPermissionWithContext(SUPER_ADMIN, USER_MANAGEMENT, "change_role",
                    Map.of("targetRole", SUPER_ADMIN))).isTrue();
        }

        @Test
        void validatePermissionReportsViolationsAndWarnings() {
            assertThat(rba.validatePermission(null, COURSE_MANAGEMENT, READ, null).getViolations())
                    .containsExactly("Role cannot be null");

            RoleBasedAccess.PermissionValidationResult blank = rba.validatePermission(STUDENT, " ", "", null);
            assertThat(blank.isValid()).isFalse();
            assertThat(blank.getViolations()).hasSize(2);

            RoleBasedAccess.PermissionValidationResult undefined = rba.validatePermission(STUDENT, "profile", READ, null);
            assertThat(undefined.isValid()).isTrue();
            assertThat(undefined.hasWarnings()).isTrue();

            RoleBasedAccess.PermissionValidationResult undefinedAction =
                    rba.validatePermission(SUPER_ADMIN, COURSE_MANAGEMENT, EXPORT, null);
            assertThat(undefinedAction.getWarnings()).singleElement().asString().contains("not defined for resource");

            RoleBasedAccess.PermissionValidationResult denied = rba.validatePermission(STUDENT, COURSE_MANAGEMENT, DELETE, null);
            assertThat(denied.isValid()).isFalse();
            assertThat(denied.hasViolations()).isTrue();
            assertThat(denied.hasWarnings()).isFalse();
        }
    }

    @Nested
    class ResolveRole {

        @Test
        void studentResolvesToStudent() {
            Student s = new Student("U1", "A", "B", "a@b.edu", null, "S1", "CS", Student.AcademicYear.SENIOR);
            assertThat(RoleBasedAccess.resolveRole(s)).isEqualTo(STUDENT);
        }

        @ParameterizedTest
        @CsvSource({"ADJUNCT, ADJUNCT_PROFESSOR", "ASSISTANT, ASSISTANT_PROFESSOR",
                "ASSOCIATE, ASSOCIATE_PROFESSOR", "FULL, FULL_PROFESSOR", "EMERITUS, FULL_PROFESSOR"})
        void professorRankMapsToRole(Professor.AcademicRank rank, UserRole expected) {
            Professor p = new Professor("U2", "P", "Q", "p@q.edu", null, "P1", "D1", rank, "AI");
            assertThat(RoleBasedAccess.resolveRole(p)).isEqualTo(expected);
        }

        @Test
        void professorWithoutRankIsAdjunct() {
            Professor p = new Professor("U2", "P", "Q", "p@q.edu", null, "P1", "D1", null, "AI");
            assertThat(RoleBasedAccess.resolveRole(p)).isEqualTo(ADJUNCT_PROFESSOR);
        }

        @ParameterizedTest
        @EnumSource(Admin.AdminLevel.class)
        void adminLevelMapsToAdminRole(Admin.AdminLevel level) {
            Admin a = new Admin("U3", "A", "D", "a@d.edu", null, "A1", level, null, "Ops");
            UserRole expected = level == Admin.AdminLevel.SUPER_ADMIN ? SUPER_ADMIN : SYSTEM_ADMINISTRATOR;
            assertThat(RoleBasedAccess.resolveRole(a)).isEqualTo(expected);
        }
    }
}
