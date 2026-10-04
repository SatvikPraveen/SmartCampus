// File location: src/main/java/patterns/ServiceFactory.java

package patterns;

import services.*;
import concurrent.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract Factory pattern implementation for creating service layer components
 * Provides different factory implementations for various service configurations
 */
public abstract class ServiceFactory {
    
    private static final Map<ServiceType, ServiceFactory> factories = new ConcurrentHashMap<>();
    private static ServiceFactory defaultFactory;
    
    static {
        // Register default factories
        registerFactory(ServiceType.STANDARD, new StandardServiceFactory());
        registerFactory(ServiceType.CONCURRENT, new ConcurrentServiceFactory());
        registerFactory(ServiceType.CACHED, new CachedServiceFactory());
        registerFactory(ServiceType.TESTING, new TestingServiceFactory());
        
        // Set default factory
        defaultFactory = factories.get(ServiceType.STANDARD);
    }
    
    /**
     * Register a service factory
     */
    public static void registerFactory(ServiceType type, ServiceFactory factory) {
        factories.put(type, factory);
    }
    
    /**
     * Get factory by type
     */
    public static ServiceFactory getFactory(ServiceType type) {
        ServiceFactory factory = factories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown service factory type: " + type);
        }
        return factory;
    }
    
    /**
     * Get default factory
     */
    public static ServiceFactory getDefaultFactory() {
        return defaultFactory;
    }
    
    /**
     * Set default factory
     */
    public static void setDefaultFactory(ServiceType type) {
        defaultFactory = getFactory(type);
    }
    
    // Abstract factory methods to be implemented by concrete factories
    public abstract AuthService createAuthService();
    public abstract StudentService createStudentService();
    public abstract ProfessorService createProfessorService();
    public abstract CourseService createCourseService();
    public abstract DepartmentService createDepartmentService();
    public abstract EnrollmentService createEnrollmentService();
    public abstract GradeService createGradeService();
    public abstract NotificationService createNotificationService();
    
    /**
     * Create a report service wired to the given core services
     */
    public abstract ReportService createReportService(StudentService studentService, ProfessorService professorService,
                                                      CourseService courseService, DepartmentService departmentService,
                                                      EnrollmentService enrollmentService, GradeService gradeService);
    
    /**
     * Create a search service wired to the given core services
     */
    public abstract SearchService createSearchService(StudentService studentService, ProfessorService professorService,
                                                      CourseService courseService, DepartmentService departmentService,
                                                      EnrollmentService enrollmentService, GradeService gradeService);
    
    /**
     * Create a report service backed by freshly created core services
     */
    public ReportService createReportService() {
        return createReportService(createStudentService(), createProfessorService(), createCourseService(),
                                   createDepartmentService(), createEnrollmentService(), createGradeService());
    }
    
    /**
     * Create a search service backed by freshly created core services
     */
    public SearchService createSearchService() {
        return createSearchService(createStudentService(), createProfessorService(), createCourseService(),
                                   createDepartmentService(), createEnrollmentService(), createGradeService());
    }
    
    // Convenience method to create all services, sharing the core services between report and search
    public ServiceBundle createAllServices() {
        StudentService studentService = createStudentService();
        ProfessorService professorService = createProfessorService();
        CourseService courseService = createCourseService();
        DepartmentService departmentService = createDepartmentService();
        EnrollmentService enrollmentService = createEnrollmentService();
        GradeService gradeService = createGradeService();
        
        return new ServiceBundle(
            createAuthService(),
            studentService,
            professorService,
            courseService,
            departmentService,
            enrollmentService,
            gradeService,
            createReportService(studentService, professorService, courseService,
                                departmentService, enrollmentService, gradeService),
            createSearchService(studentService, professorService, courseService,
                                departmentService, enrollmentService, gradeService),
            createNotificationService()
        );
    }
    
    // Standard implementation of the service factory
    public static class StandardServiceFactory extends ServiceFactory {
        
        @Override
        public AuthService createAuthService() {
            return AuthService.getInstance();
        }
        
        @Override
        public StudentService createStudentService() {
            return new StudentService();
        }
        
        @Override
        public ProfessorService createProfessorService() {
            return new ProfessorService();
        }
        
        @Override
        public CourseService createCourseService() {
            return new CourseService();
        }
        
        @Override
        public DepartmentService createDepartmentService() {
            return new DepartmentService();
        }
        
        @Override
        public EnrollmentService createEnrollmentService() {
            return new EnrollmentService();
        }
        
        @Override
        public GradeService createGradeService() {
            return new GradeService();
        }
        
        @Override
        public ReportService createReportService(StudentService studentService, ProfessorService professorService,
                                                 CourseService courseService, DepartmentService departmentService,
                                                 EnrollmentService enrollmentService, GradeService gradeService) {
            return new ReportService(studentService, professorService, courseService,
                                     departmentService, enrollmentService, gradeService);
        }
        
        @Override
        public SearchService createSearchService(StudentService studentService, ProfessorService professorService,
                                                 CourseService courseService, DepartmentService departmentService,
                                                 EnrollmentService enrollmentService, GradeService gradeService) {
            return new SearchService(studentService, professorService, courseService,
                                     departmentService, enrollmentService, gradeService);
        }
        
        @Override
        public NotificationService createNotificationService() {
            return new NotificationService();
        }
    }
    
    // Concurrent implementation with enhanced performance
    public static class ConcurrentServiceFactory extends ServiceFactory {
        
        // Concurrent processors
        private final EnrollmentProcessor enrollmentProcessor = new EnrollmentProcessor(
            new EnrollmentService(),
            new NotificationService()
        );
        private final AsyncNotificationSender notificationSender = new AsyncNotificationSender(
            new NotificationService(), 4, 100
        );
        private final BatchProcessor batchProcessor = new BatchProcessor(4, 100, 5);
        
        @Override
        public AuthService createAuthService() {
            return AuthService.getInstance();
        }
        
        @Override
        public StudentService createStudentService() {
            return new ConcurrentStudentService(batchProcessor);
        }
        
        @Override
        public ProfessorService createProfessorService() {
            return new ConcurrentProfessorService(batchProcessor);
        }
        
        @Override
        public CourseService createCourseService() {
            return new ConcurrentCourseService(batchProcessor);
        }
        
        @Override
        public DepartmentService createDepartmentService() {
            return new DepartmentService();
        }
        
        @Override
        public EnrollmentService createEnrollmentService() {
            return new ConcurrentEnrollmentService(enrollmentProcessor);
        }
        
        @Override
        public GradeService createGradeService() {
            return new ConcurrentGradeService(batchProcessor);
        }
        
        @Override
        public ReportService createReportService(StudentService studentService, ProfessorService professorService,
                                                 CourseService courseService, DepartmentService departmentService,
                                                 EnrollmentService enrollmentService, GradeService gradeService) {
            return new ConcurrentReportService(studentService, professorService, courseService,
                                     departmentService, enrollmentService, gradeService, batchProcessor);
        }
        
        @Override
        public SearchService createSearchService(StudentService studentService, ProfessorService professorService,
                                                 CourseService courseService, DepartmentService departmentService,
                                                 EnrollmentService enrollmentService, GradeService gradeService) {
            return new ConcurrentSearchService(studentService, professorService, courseService,
                                     departmentService, enrollmentService, gradeService);
        }
        
        @Override
        public NotificationService createNotificationService() {
            return new AsyncNotificationService(notificationSender);
        }
    }
    
    // Cached implementation for improved performance
    public static class CachedServiceFactory extends ServiceFactory {
        
        // Cache managers
        private final Map<String, Object> serviceCache = new ConcurrentHashMap<>();
        
        @Override
        public AuthService createAuthService() {
            return (AuthService) serviceCache.computeIfAbsent("auth", k -> AuthService.getInstance());
        }
        
        @Override
        public StudentService createStudentService() {
            return (StudentService) serviceCache.computeIfAbsent("student", k -> new CachedStudentService());
        }
        
        @Override
        public ProfessorService createProfessorService() {
            return (ProfessorService) serviceCache.computeIfAbsent("professor", k -> new CachedProfessorService());
        }
        
        @Override
        public CourseService createCourseService() {
            return (CourseService) serviceCache.computeIfAbsent("course", k -> new CachedCourseService());
        }
        
        @Override
        public DepartmentService createDepartmentService() {
            return (DepartmentService) serviceCache.computeIfAbsent("department", k -> new CachedDepartmentService());
        }
        
        @Override
        public EnrollmentService createEnrollmentService() {
            return (EnrollmentService) serviceCache.computeIfAbsent("enrollment", k -> new CachedEnrollmentService());
        }
        
        @Override
        public GradeService createGradeService() {
            return (GradeService) serviceCache.computeIfAbsent("grade", k -> new CachedGradeService());
        }
        
        @Override
        public ReportService createReportService(StudentService studentService, ProfessorService professorService,
                                                 CourseService courseService, DepartmentService departmentService,
                                                 EnrollmentService enrollmentService, GradeService gradeService) {
            return (ReportService) serviceCache.computeIfAbsent("report",
                k -> new CachedReportService(studentService, professorService, courseService,
                                     departmentService, enrollmentService, gradeService));
        }
        
        @Override
        public SearchService createSearchService(StudentService studentService, ProfessorService professorService,
                                                 CourseService courseService, DepartmentService departmentService,
                                                 EnrollmentService enrollmentService, GradeService gradeService) {
            return (SearchService) serviceCache.computeIfAbsent("search",
                k -> new CachedSearchService(studentService, professorService, courseService,
                                     departmentService, enrollmentService, gradeService));
        }
        
        @Override
        public NotificationService createNotificationService() {
            return (NotificationService) serviceCache.computeIfAbsent("notification",
                k -> new CachedNotificationService());
        }
    }
    
    // Testing implementation with mock services
    public static class TestingServiceFactory extends ServiceFactory {
        
        @Override
        public AuthService createAuthService() {
            return AuthService.getInstance();
        }
        
        @Override
        public StudentService createStudentService() {
            return new MockStudentService();
        }
        
        @Override
        public ProfessorService createProfessorService() {
            return new MockProfessorService();
        }
        
        @Override
        public CourseService createCourseService() {
            return new MockCourseService();
        }
        
        @Override
        public DepartmentService createDepartmentService() {
            return new MockDepartmentService();
        }
        
        @Override
        public EnrollmentService createEnrollmentService() {
            return new MockEnrollmentService();
        }
        
        @Override
        public GradeService createGradeService() {
            return new MockGradeService();
        }
        
        @Override
        public ReportService createReportService(StudentService studentService, ProfessorService professorService,
                                                 CourseService courseService, DepartmentService departmentService,
                                                 EnrollmentService enrollmentService, GradeService gradeService) {
            return new MockReportService(studentService, professorService, courseService,
                                     departmentService, enrollmentService, gradeService);
        }
        
        @Override
        public SearchService createSearchService(StudentService studentService, ProfessorService professorService,
                                                 CourseService courseService, DepartmentService departmentService,
                                                 EnrollmentService enrollmentService, GradeService gradeService) {
            return new MockSearchService(studentService, professorService, courseService,
                                     departmentService, enrollmentService, gradeService);
        }
        
        @Override
        public NotificationService createNotificationService() {
            return new MockNotificationService();
        }
    }
    
    // Service type enumeration
    public enum ServiceType {
        STANDARD,       // Basic implementation
        CONCURRENT,     // High-performance concurrent implementation
        CACHED,         // Cached implementation for improved performance
        TESTING,        // Mock implementation for testing
        DISTRIBUTED,    // Distributed implementation (placeholder)
        SECURE          // Security-enhanced implementation (placeholder)
    }
    
    // Service bundle to hold all services
    public static class ServiceBundle {
        private final AuthService authService;
        private final StudentService studentService;
        private final ProfessorService professorService;
        private final CourseService courseService;
        private final DepartmentService departmentService;
        private final EnrollmentService enrollmentService;
        private final GradeService gradeService;
        private final ReportService reportService;
        private final SearchService searchService;
        private final NotificationService notificationService;
        
        public ServiceBundle(AuthService authService, StudentService studentService,
                           ProfessorService professorService, CourseService courseService,
                           DepartmentService departmentService, EnrollmentService enrollmentService,
                           GradeService gradeService, ReportService reportService,
                           SearchService searchService, NotificationService notificationService) {
            this.authService = authService;
            this.studentService = studentService;
            this.professorService = professorService;
            this.courseService = courseService;
            this.departmentService = departmentService;
            this.enrollmentService = enrollmentService;
            this.gradeService = gradeService;
            this.reportService = reportService;
            this.searchService = searchService;
            this.notificationService = notificationService;
        }
        
        // Getters
        public AuthService getAuthService() { return authService; }
        public StudentService getStudentService() { return studentService; }
        public ProfessorService getProfessorService() { return professorService; }
        public CourseService getCourseService() { return courseService; }
        public DepartmentService getDepartmentService() { return departmentService; }
        public EnrollmentService getEnrollmentService() { return enrollmentService; }
        public GradeService getGradeService() { return gradeService; }
        public ReportService getReportService() { return reportService; }
        public SearchService getSearchService() { return searchService; }
        public NotificationService getNotificationService() { return notificationService; }
        
        /**
         * Initialize all services
         */
        public void initializeServices() {
            // Perform any necessary initialization
            System.out.println("Initializing all services...");
            // Add initialization logic as needed
        }
        
        /**
         * Shutdown all services
         */
        public void shutdownServices() {
            System.out.println("Shutting down all services...");
            // Add cleanup logic as needed
        }
    }
    
    // Placeholder classes for enhanced service implementations
    // In a real implementation, these would be fully implemented classes
    
    private static class ConcurrentStudentService extends StudentService {
        private final BatchProcessor batchProcessor;
        
        public ConcurrentStudentService(BatchProcessor batchProcessor) {
            super();
            this.batchProcessor = batchProcessor;
        }
    }
    
    private static class ConcurrentProfessorService extends ProfessorService {
        private final BatchProcessor batchProcessor;
        
        public ConcurrentProfessorService(BatchProcessor batchProcessor) {
            super();
            this.batchProcessor = batchProcessor;
        }
    }
    
    private static class ConcurrentCourseService extends CourseService {
        private final BatchProcessor batchProcessor;
        
        public ConcurrentCourseService(BatchProcessor batchProcessor) {
            super();
            this.batchProcessor = batchProcessor;
        }
    }
    
    private static class ConcurrentEnrollmentService extends EnrollmentService {
        private final EnrollmentProcessor enrollmentProcessor;
        
        public ConcurrentEnrollmentService(EnrollmentProcessor processor) {
            super();
            this.enrollmentProcessor = processor;
        }
    }
    
    private static class ConcurrentGradeService extends GradeService {
        private final BatchProcessor batchProcessor;
        
        public ConcurrentGradeService(BatchProcessor batchProcessor) {
            super();
            this.batchProcessor = batchProcessor;
        }
    }
    
    private static class ConcurrentReportService extends ReportService {
        private final BatchProcessor batchProcessor;
        
        public ConcurrentReportService(StudentService studentService, ProfessorService professorService,
                       CourseService courseService, DepartmentService departmentService,
                       EnrollmentService enrollmentService, GradeService gradeService,
                       BatchProcessor batchProcessor) {
            super(studentService, professorService, courseService, departmentService, enrollmentService, gradeService);
            this.batchProcessor = batchProcessor;
        }
    }
    
    private static class ConcurrentSearchService extends SearchService {
        public ConcurrentSearchService(StudentService studentService, ProfessorService professorService,
                       CourseService courseService, DepartmentService departmentService,
                       EnrollmentService enrollmentService, GradeService gradeService) {
            super(studentService, professorService, courseService, departmentService, enrollmentService, gradeService);
        }
    }
    
    private static class AsyncNotificationService extends NotificationService {
        private final AsyncNotificationSender notificationSender;
        
        public AsyncNotificationService(AsyncNotificationSender sender) {
            this.notificationSender = sender;
        }
    }
    
    // Cached service implementations (placeholder)
    private static class CachedStudentService extends StudentService {
    }
    
    private static class CachedProfessorService extends ProfessorService {
    }
    
    private static class CachedCourseService extends CourseService {
    }
    
    private static class CachedDepartmentService extends DepartmentService {
    }
    
    private static class CachedEnrollmentService extends EnrollmentService {
    }
    
    private static class CachedGradeService extends GradeService {
    }
    
    private static class CachedReportService extends ReportService {
        public CachedReportService(StudentService studentService, ProfessorService professorService,
                       CourseService courseService, DepartmentService departmentService,
                       EnrollmentService enrollmentService, GradeService gradeService) {
            super(studentService, professorService, courseService, departmentService, enrollmentService, gradeService);
        }
    }
    
    private static class CachedSearchService extends SearchService {
        public CachedSearchService(StudentService studentService, ProfessorService professorService,
                       CourseService courseService, DepartmentService departmentService,
                       EnrollmentService enrollmentService, GradeService gradeService) {
            super(studentService, professorService, courseService, departmentService, enrollmentService, gradeService);
        }
    }
    
    private static class CachedNotificationService extends NotificationService {
    }
    
    // Mock service implementations for testing
    private static class MockStudentService extends StudentService {
    }
    
    private static class MockProfessorService extends ProfessorService {
    }
    
    private static class MockCourseService extends CourseService {
    }
    
    private static class MockDepartmentService extends DepartmentService {
    }
    
    private static class MockEnrollmentService extends EnrollmentService {
    }
    
    private static class MockGradeService extends GradeService {
    }
    
    private static class MockReportService extends ReportService {
        public MockReportService(StudentService studentService, ProfessorService professorService,
                       CourseService courseService, DepartmentService departmentService,
                       EnrollmentService enrollmentService, GradeService gradeService) {
            super(studentService, professorService, courseService, departmentService, enrollmentService, gradeService);
        }
    }
    
    private static class MockSearchService extends SearchService {
        public MockSearchService(StudentService studentService, ProfessorService professorService,
                       CourseService courseService, DepartmentService departmentService,
                       EnrollmentService enrollmentService, GradeService gradeService) {
            super(studentService, professorService, courseService, departmentService, enrollmentService, gradeService);
        }
    }
    
    private static class MockNotificationService extends NotificationService {
    }
}
