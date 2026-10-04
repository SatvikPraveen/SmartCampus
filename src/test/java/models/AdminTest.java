package models;

import models.Admin.AdminLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminTest {

    private static Admin admin(AdminLevel level) {
        return new Admin("U5", "Grace", "Hopper", "grace@example.edu", null,
                "A1", level, Admin.Department.REGISTRAR, "Registrar");
    }

    @Test
    void defaultCapabilitiesPerLevel() {
        Admin junior = admin(AdminLevel.JUNIOR_ADMIN);
        assertThat(junior.hasSystemAccess()).isTrue();
        assertThat(junior.canViewReports()).isTrue();
        assertThat(junior.canModifyData()).isFalse();
        assertThat(junior.canManageUsers()).isFalse();
        assertThat(junior.getPermissions()).containsExactly("READ_STUDENT_DATA", "READ_COURSE_DATA");

        Admin senior = admin(AdminLevel.SENIOR_ADMIN);
        assertThat(senior.canModifyData()).isTrue();
        assertThat(senior.canManageUsers()).isFalse();

        Admin superAdmin = admin(AdminLevel.SUPER_ADMIN);
        assertThat(superAdmin.canManageUsers()).isTrue();
        assertThat(superAdmin.hasPermission("MANAGE_ADMINS")).isTrue();
    }

    @Test
    void promotionWalksTheLevelsAndStopsAtTheTop() {
        Admin a = admin(AdminLevel.JUNIOR_ADMIN);
        assertThat(a.promoteAdmin()).isTrue();
        assertThat(a.getAdminLevel()).isEqualTo(AdminLevel.SENIOR_ADMIN);
        assertThat(a.canModifyData()).isTrue();
        assertThat(a.promoteAdmin()).isTrue();
        assertThat(a.promoteAdmin()).isTrue();
        assertThat(a.getAdminLevel()).isEqualTo(AdminLevel.SUPER_ADMIN);
        assertThat(a.promoteAdmin()).isFalse();
        assertThat(a.getAdminLevel()).isEqualTo(AdminLevel.SUPER_ADMIN);
    }

    @Test
    @DisplayName("demotion revokes capabilities of the former level (regression)")
    void demotionRevokesCapabilities() {
        Admin a = admin(AdminLevel.SYSTEM_ADMIN);
        assertThat(a.canManageUsers()).isTrue();
        a.setAdminLevel(AdminLevel.JUNIOR_ADMIN);
        assertThat(a.canManageUsers()).isFalse();
        assertThat(a.canModifyData()).isFalse();
        assertThat(a.hasPermission("FULL_SYSTEM_ACCESS")).isFalse();
        assertThat(a.hasSystemAccess()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(AdminLevel.class)
    @DisplayName("capabilities depend only on the current level, not on history")
    void capabilitiesAreAFunctionOfLevel(AdminLevel level) {
        Admin fresh = admin(level);
        Admin changed = admin(AdminLevel.SUPER_ADMIN);
        changed.setAdminLevel(level);
        assertThat(changed.getPermissions()).containsExactlyElementsOf(fresh.getPermissions());
        assertThat(changed.canManageUsers()).isEqualTo(fresh.canManageUsers());
        assertThat(changed.canModifyData()).isEqualTo(fresh.canModifyData());
        assertThat(changed.canViewReports()).isEqualTo(fresh.canViewReports());
        assertThat(changed.hasSystemAccess()).isEqualTo(fresh.hasSystemAccess());
    }

    @Test
    @DisplayName("every constructor starts with 20 vacation days (regression)")
    void vacationDaysDefault() {
        assertThat(new Admin().getVacationDays()).isEqualTo(20);
        assertThat(admin(AdminLevel.JUNIOR_ADMIN).getVacationDays()).isEqualTo(20);
        Admin full = new Admin("U6", "Ada", "L", "ada@example.edu", null, "A2", AdminLevel.SENIOR_ADMIN,
                Admin.Department.FINANCE, "CFO", LocalDateTime.of(2020, 1, 1, 9, 0), "A1", 100000, "Bob");
        assertThat(full.getVacationDays()).isEqualTo(20);
    }

    @Test
    void vacationDaysCannotBeOverdrawn() {
        Admin a = admin(AdminLevel.JUNIOR_ADMIN);
        assertThat(a.useVacationDays(0)).isFalse();
        assertThat(a.useVacationDays(21)).isFalse();
        assertThat(a.useVacationDays(20)).isTrue();
        assertThat(a.getVacationDays()).isZero();
        a.addVacationDays(-5);
        assertThat(a.getVacationDays()).isZero();
        a.addVacationDays(3);
        assertThat(a.getVacationDays()).isEqualTo(3);
    }

    @Test
    void permissionsAndDepartmentsAreManagedThroughTheApiOnly() {
        Admin a = admin(AdminLevel.JUNIOR_ADMIN);
        a.addPermission("EXPORT");
        a.addPermission("EXPORT");
        a.addPermission(" ");
        assertThat(a.getPermissions()).containsExactly("READ_STUDENT_DATA", "READ_COURSE_DATA", "EXPORT");
        assertThatThrownBy(() -> a.getPermissions().add("HACK")).isInstanceOf(UnsupportedOperationException.class);
        a.removePermission("EXPORT");
        assertThat(a.hasPermission("EXPORT")).isFalse();

        a.addManagedDepartment("D1");
        a.addManagedDepartment("D1");
        assertThat(a.getManagedDepartments()).containsExactly("D1");
        assertThat(a.managesDepartment("D1")).isTrue();
        assertThatThrownBy(() -> a.getManagedDepartments().clear()).isInstanceOf(UnsupportedOperationException.class);
        a.removeManagedDepartment("D1");
        assertThat(a.managesDepartment("D1")).isFalse();
    }

    @Test
    void salaryCannotBeNegative() {
        Admin a = admin(AdminLevel.JUNIOR_ADMIN);
        a.setSalary(50000);
        a.setSalary(-1);
        assertThat(a.getSalary()).isEqualTo(50000);
    }

    @Test
    @DisplayName("toString works on a default-constructed admin (regression)")
    void toStringIsNullSafe() {
        assertThat(new Admin().toString()).startsWith("Admin{");
        assertThat(admin(AdminLevel.SENIOR_ADMIN).toString())
                .contains("Senior Admin").contains("Registrar").contains("Grace Hopper");
    }

    @Test
    void roleAndEquality() {
        Admin a = admin(AdminLevel.JUNIOR_ADMIN);
        assertThat(a.getRole()).isEqualTo("Admin");
        assertThat(a).isEqualTo(admin(AdminLevel.SUPER_ADMIN));
    }
}
