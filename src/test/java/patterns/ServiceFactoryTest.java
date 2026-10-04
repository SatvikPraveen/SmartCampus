package patterns;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import patterns.ServiceFactory.CachedServiceFactory;
import patterns.ServiceFactory.ConcurrentServiceFactory;
import patterns.ServiceFactory.ServiceBundle;
import patterns.ServiceFactory.ServiceType;
import patterns.ServiceFactory.StandardServiceFactory;
import patterns.ServiceFactory.TestingServiceFactory;
import services.AuthService;
import services.CourseService;
import services.DepartmentService;
import services.EnrollmentService;
import services.GradeService;
import services.NotificationService;
import services.ProfessorService;
import services.ReportService;
import services.SearchService;
import services.StudentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ServiceFactory keeps a static registry and default. Tests that change either restore the original
 * registrations in {@link #restoreRegistry()}.
 */
class ServiceFactoryTest {

    private ServiceFactory replacedCached;

    @AfterEach
    void restoreRegistry() {
        if (replacedCached != null) {
            ServiceFactory.registerFactory(ServiceType.CACHED, replacedCached);
            replacedCached = null;
        }
        ServiceFactory.setDefaultFactory(ServiceType.STANDARD);
    }

    /** Installs a fresh cached factory (so cached instances from other tests are not observed). */
    private ServiceFactory freshCached() {
        replacedCached = ServiceFactory.getFactory(ServiceType.CACHED);
        ServiceFactory fresh = new CachedServiceFactory();
        ServiceFactory.registerFactory(ServiceType.CACHED, fresh);
        return fresh;
    }

    @Nested
    class Registry {

        @Test
        void builtInFactoriesAreRegistered() {
            assertThat(ServiceFactory.getFactory(ServiceType.STANDARD)).isInstanceOf(StandardServiceFactory.class);
            assertThat(ServiceFactory.getFactory(ServiceType.CONCURRENT)).isInstanceOf(ConcurrentServiceFactory.class);
            assertThat(ServiceFactory.getFactory(ServiceType.CACHED)).isInstanceOf(CachedServiceFactory.class);
            assertThat(ServiceFactory.getFactory(ServiceType.TESTING)).isInstanceOf(TestingServiceFactory.class);
        }

        @Test
        void factoriesAreSingletonsPerType() {
            assertThat(ServiceFactory.getFactory(ServiceType.STANDARD)).isSameAs(ServiceFactory.getFactory(ServiceType.STANDARD));
        }

        @Test
        void placeholderTypesAreNotRegistered() {
            assertThatThrownBy(() -> ServiceFactory.getFactory(ServiceType.DISTRIBUTED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DISTRIBUTED");
            assertThatThrownBy(() -> ServiceFactory.setDefaultFactory(ServiceType.SECURE))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void defaultFactoryIsStandardAndCanBeSwitched() {
            assertThat(ServiceFactory.getDefaultFactory()).isSameAs(ServiceFactory.getFactory(ServiceType.STANDARD));

            ServiceFactory.setDefaultFactory(ServiceType.TESTING);

            assertThat(ServiceFactory.getDefaultFactory()).isSameAs(ServiceFactory.getFactory(ServiceType.TESTING));
        }

        @Test
        void customFactoryCanReplaceRegistration() {
            ServiceFactory fresh = freshCached();

            assertThat(ServiceFactory.getFactory(ServiceType.CACHED)).isSameAs(fresh);
            ServiceFactory.setDefaultFactory(ServiceType.CACHED);
            assertThat(ServiceFactory.getDefaultFactory()).isSameAs(fresh);
        }
    }

    @Nested
    class Creation {

        @ParameterizedTest
        @EnumSource(value = ServiceType.class, names = {"STANDARD", "CONCURRENT", "CACHED", "TESTING"})
        void everyFactoryCreatesEveryService(ServiceType type) {
            ServiceFactory factory = ServiceFactory.getFactory(type);

            assertThat(factory.createAuthService()).isSameAs(AuthService.getInstance());
            assertThat(factory.createStudentService()).isInstanceOf(StudentService.class);
            assertThat(factory.createProfessorService()).isInstanceOf(ProfessorService.class);
            assertThat(factory.createCourseService()).isInstanceOf(CourseService.class);
            assertThat(factory.createDepartmentService()).isInstanceOf(DepartmentService.class);
            assertThat(factory.createEnrollmentService()).isInstanceOf(EnrollmentService.class);
            assertThat(factory.createGradeService()).isInstanceOf(GradeService.class);
            assertThat(factory.createNotificationService()).isInstanceOf(NotificationService.class);
            assertThat(factory.createReportService()).isInstanceOf(ReportService.class);
            assertThat(factory.createSearchService()).isInstanceOf(SearchService.class);
        }

        @Test
        void standardFactoryCreatesPlainNewInstancesEachTime() {
            ServiceFactory standard = ServiceFactory.getFactory(ServiceType.STANDARD);

            StudentService first = standard.createStudentService();

            assertThat(first.getClass()).isEqualTo(StudentService.class);
            assertThat(standard.createStudentService()).isNotSameAs(first);
            assertThat(standard.createCourseService()).isNotSameAs(standard.createCourseService());
            assertThat(standard.createReportService().getClass()).isEqualTo(ReportService.class);
        }

        @Test
        void specializedFactoriesProduceSubclasses() {
            assertThat(ServiceFactory.getFactory(ServiceType.CONCURRENT).createStudentService().getClass())
                .isNotEqualTo(StudentService.class);
            assertThat(ServiceFactory.getFactory(ServiceType.TESTING).createGradeService().getClass())
                .isNotEqualTo(GradeService.class);
            assertThat(ServiceFactory.getFactory(ServiceType.CONCURRENT).createDepartmentService().getClass())
                .isEqualTo(DepartmentService.class);
        }

        @Test
        void concurrentAndTestingFactoriesCreateNewInstancesEachTime() {
            ServiceFactory concurrent = ServiceFactory.getFactory(ServiceType.CONCURRENT);
            ServiceFactory testing = ServiceFactory.getFactory(ServiceType.TESTING);

            assertThat(concurrent.createEnrollmentService()).isNotSameAs(concurrent.createEnrollmentService());
            assertThat(testing.createEnrollmentService()).isNotSameAs(testing.createEnrollmentService());
        }

        @Test
        void cachedFactoryReturnsSameInstancePerService() {
            ServiceFactory cached = freshCached();

            assertThat(cached.createStudentService()).isSameAs(cached.createStudentService());
            assertThat(cached.createProfessorService()).isSameAs(cached.createProfessorService());
            assertThat(cached.createCourseService()).isSameAs(cached.createCourseService());
            assertThat(cached.createDepartmentService()).isSameAs(cached.createDepartmentService());
            assertThat(cached.createEnrollmentService()).isSameAs(cached.createEnrollmentService());
            assertThat(cached.createGradeService()).isSameAs(cached.createGradeService());
            assertThat(cached.createNotificationService()).isSameAs(cached.createNotificationService());
            assertThat(cached.createReportService()).isSameAs(cached.createReportService());
            assertThat(cached.createSearchService()).isSameAs(cached.createSearchService());
            assertThat(cached.createAuthService()).isSameAs(cached.createAuthService());
        }

        @Test
        void separateCachedFactoriesDoNotShareInstances() {
            assertThat(new CachedServiceFactory().createStudentService())
                .isNotSameAs(new CachedServiceFactory().createStudentService());
        }

        @Test
        void cachedStateIsVisibleThroughEveryLookup() {
            ServiceFactory cached = freshCached();
            cached.createStudentService().addStudent(new models.Student("STU000001", "Ada", "Lovelace",
                "ada@university.edu", null, "STU000001", "CS", models.Student.AcademicYear.FRESHMAN));

            assertThat(cached.createStudentService().getStudentById("STU000001")).isPresent();
        }
    }

    @Nested
    class Bundles {

        @ParameterizedTest
        @EnumSource(value = ServiceType.class, names = {"STANDARD", "CONCURRENT", "CACHED", "TESTING"})
        void bundleExposesEveryService(ServiceType type) {
            ServiceBundle bundle = ServiceFactory.getFactory(type).createAllServices();

            assertThat(bundle.getAuthService()).isSameAs(AuthService.getInstance());
            assertThat(bundle.getStudentService()).isNotNull();
            assertThat(bundle.getProfessorService()).isNotNull();
            assertThat(bundle.getCourseService()).isNotNull();
            assertThat(bundle.getDepartmentService()).isNotNull();
            assertThat(bundle.getEnrollmentService()).isNotNull();
            assertThat(bundle.getGradeService()).isNotNull();
            assertThat(bundle.getReportService()).isNotNull();
            assertThat(bundle.getSearchService()).isNotNull();
            assertThat(bundle.getNotificationService()).isNotNull();
        }

        @Test
        void cachedBundleReusesCachedServices() {
            ServiceFactory cached = freshCached();

            ServiceBundle bundle = cached.createAllServices();

            assertThat(bundle.getStudentService()).isSameAs(cached.createStudentService());
            assertThat(bundle.getGradeService()).isSameAs(cached.createGradeService());
            assertThat(bundle.getReportService()).isSameAs(cached.createReportService());
        }

        @Test
        void lifecycleHooksAreCallable() {
            ServiceBundle bundle = ServiceFactory.getFactory(ServiceType.STANDARD).createAllServices();

            bundle.initializeServices();
            bundle.shutdownServices();

            assertThat(bundle.getStudentService()).isNotNull();
        }
    }
}
