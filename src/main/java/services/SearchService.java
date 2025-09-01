// File: src/main/java/services/SearchService.java
package services;

import models.*;
import interfaces.Searchable;
import utils.ValidationUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * SearchService class providing unified search functionality across all entities.
 * This service aggregates search capabilities from all other services and provides
 * advanced search features including full-text search, fuzzy matching, and faceted search.
 * 
 * Key Java concepts demonstrated:
 * - Generic type handling and wildcards
 * - Advanced Stream API operations
 * - Functional programming with complex predicates
 * - Search algorithms and text processing
 * - Concurrent search operations
 * - Search result ranking and relevance scoring
 * - Faceted search and filtering
 */
public class SearchService {
    
    // Service dependencies
    private final StudentService studentService;
    private final ProfessorService professorService;
    private final CourseService courseService;
    private final DepartmentService departmentService;
    private final EnrollmentService enrollmentService;
    private final GradeService gradeService;
    
    // Search indices and caching
    private final Map<String, List<SearchResult<?>>> searchCache;
    private final Map<String, Set<String>> searchIndex;
    private final Map<Class<?>, Searchable<?>> searchableServices;
    
    // Search configuration
    private final int maxSearchResults = 1000;
    private final int defaultPageSize = 20;
    private final double fuzzyMatchThreshold = 0.7;
    
    /**
     * Constructor with service dependencies.
     */
    public SearchService(StudentService studentService, ProfessorService professorService,
                        CourseService courseService, DepartmentService departmentService,
                        EnrollmentService enrollmentService, GradeService gradeService) {
        this.studentService = studentService;
        this.professorService = professorService;
        this.courseService = courseService;
        this.departmentService = departmentService;
        this.enrollmentService = enrollmentService;
        this.gradeService = gradeService;
        
        this.searchCache = new ConcurrentHashMap<>();
        this.searchIndex = new ConcurrentHashMap<>();
        this.searchableServices = initializeSearchableServices();
        
        buildSearchIndex();
    }
    
    // Universal search methods
    
    /**
     * Perform universal search across all entities.
     * 
     * @param query The search query
     * @return UniversalSearchResult containing results from all entity types
     */
    public UniversalSearchResult searchAll(String query) {
        if (!ValidationUtil.isValidString(query)) {
            return new UniversalSearchResult(query, Collections.emptyMap());
        }
        
        // Search across all entity types concurrently
        CompletableFuture<List<Student>> studentFuture = CompletableFuture
                .supplyAsync(() -> studentService.search(query));
        
        CompletableFuture<List<Professor>> professorFuture = CompletableFuture
                .supplyAsync(() -> professorService.search(query));
        
        CompletableFuture<List<Course>> courseFuture = CompletableFuture
                .supplyAsync(() -> courseService.search(query));
        
        CompletableFuture<List<Department>> departmentFuture = CompletableFuture
                .supplyAsync(() -> departmentService.search(query));
        
        CompletableFuture<List<Enrollment>> enrollmentFuture = CompletableFuture
                .supplyAsync(() -> enrollmentService.search(query));
        
        CompletableFuture<List<Grade>> gradeFuture = CompletableFuture
                .supplyAsync(() -> gradeService.search(query));
        
        // Combine all results
        Map<String, List<?>> results = new HashMap<>();
        
        try {
            results.put("students", studentFuture.get());
            results.put("professors", professorFuture.get());
            results.put("courses", courseFuture.get());
            results.put("departments", departmentFuture.get());
            results.put("enrollments", enrollmentFuture.get());
            results.put("grades", gradeFuture.get());
        } catch (Exception e) {
            // Handle search errors gracefully
            results.put("error", List.of("Search operation failed: " + e.getMessage()));
        }
        
        return new UniversalSearchResult(query, results);
    }
    
    /**
     * Perform advanced search with multiple criteria.
     * 
     * @param criteria Advanced search criteria
     * @return AdvancedSearchResult with filtered and ranked results
     */
    public AdvancedSearchResult advancedSearch(AdvancedSearchCriteria criteria) {
        Map<String, List<?>> results = new HashMap<>();
        
        // Search each entity type with specific criteria
        if (criteria.includeStudents()) {
            results.put("students", searchStudentsAdvanced(criteria));
        }
        
        if (criteria.includeProfessors()) {
            results.put("professors", searchProfessorsAdvanced(criteria));
        }
        
        if (criteria.includeCourses()) {
            results.put("courses", searchCoursesAdvanced(criteria));
        }
        
        if (criteria.includeDepartments()) {
            results.put("departments", searchDepartmentsAdvanced(criteria));
        }
        
        if (criteria.includeEnrollments()) {
            results.put("enrollments", searchEnrollmentsAdvanced(criteria));
        }
        
        if (criteria.includeGrades()) {
            results.put("grades", searchGradesAdvanced(criteria));
        }
        
        return new AdvancedSearchResult(criteria, results, calculateRelevanceScores(results, criteria));
    }
    
    /**
     * Perform fuzzy search with similarity matching.
     * 
     * @param query The search query
     * @param threshold Similarity threshold (0.0 to 1.0)
     * @return FuzzySearchResult with similarity scores
     */
    public FuzzySearchResult fuzzySearch(String query, double threshold) {
        if (!ValidationUtil.isValidString(query) || threshold < 0.0 || threshold > 1.0) {
            return new FuzzySearchResult(query, threshold, Collections.emptyMap());
        }
        
        Map<String, List<FuzzyMatch<?>>> fuzzyResults = new HashMap<>();
        
        // Perform fuzzy matching for each entity type
        fuzzyResults.put("students", fuzzySearchStudents(query, threshold));
        fuzzyResults.put("professors", fuzzySearchProfessors(query, threshold));
        fuzzyResults.put("courses", fuzzySearchCourses(query, threshold));
        fuzzyResults.put("departments", fuzzySearchDepartments(query, threshold));
        
        return new FuzzySearchResult(query, threshold, fuzzyResults);
    }
    
    /**
     * Perform faceted search with filters and aggregations.
     * 
     * @param query The base search query
     * @param facets The facets to apply
     * @return FacetedSearchResult with facet counts and filtered results
     */
    public FacetedSearchResult facetedSearch(String query, Map<String, Set<String>> facets) {
        // Start with universal search
        UniversalSearchResult baseResults = searchAll(query);
        
        // Apply facet filters
        Map<String, List<?>> filteredResults = new HashMap<>();
        Map<String, Map<String, Long>> facetCounts = new HashMap<>();
        
        for (Map.Entry<String, List<?>> entry : baseResults.getResults().entrySet()) {
            String entityType = entry.getKey();
            List<?> entities = entry.getValue();
            
            // Apply facet filters for this entity type
            List<?> filtered = applyFacetFilters(entities, facets.getOrDefault(entityType, Collections.emptySet()));
            filteredResults.put(entityType, filtered);
            
            // Calculate facet counts
            facetCounts.put(entityType, calculateFacetCounts(entities, entityType));
        }
        
        return new FacetedSearchResult(query, facets, filteredResults, facetCounts);
    }
    
    /**
     * Get search suggestions based on partial query.
     * 
     * @param partialQuery The partial search query
     * @param maxSuggestions Maximum number of suggestions
     * @return List of search suggestions
     */
    public List<SearchSuggestion> getSearchSuggestions(String partialQuery, int maxSuggestions) {
        if (!ValidationUtil.isValidString(partialQuery)) {
            return Collections.emptyList();
        }
        
        Set<SearchSuggestion> suggestions = new HashSet<>();
        
        // Get suggestions from each service
        suggestions.addAll(getServiceSuggestions("students", studentService, partialQuery, maxSuggestions / 6));
        suggestions.addAll(getServiceSuggestions("professors", professorService, partialQuery, maxSuggestions / 6));
        suggestions.addAll(getServiceSuggestions("courses", courseService, partialQuery, maxSuggestions / 6));
        suggestions.addAll(getServiceSuggestions("departments", departmentService, partialQuery, maxSuggestions / 6));
        suggestions.addAll(getServiceSuggestions("enrollments", enrollmentService, partialQuery, maxSuggestions / 6));
        suggestions.addAll(getServiceSuggestions("grades", gradeService, partialQuery, maxSuggestions / 6));
        
        return suggestions.stream()
                .sorted(Comparator.comparing(SearchSuggestion::getScore).reversed())
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    /**
     * Search with auto-complete functionality.
     * 
     * @param query The search query
     * @param includePartial Whether to include partial matches
     * @return AutoCompleteResult with suggestions and results
     */
    public AutoCompleteResult searchWithAutoComplete(String query, boolean includePartial) {
        List<SearchSuggestion> suggestions = getSearchSuggestions(query, 10);
        
        UniversalSearchResult searchResults = null;
        if (query.length() >= 3) { // Only search for queries with 3+ characters
            searchResults = searchAll(query);
        }
        
        return new AutoCompleteResult(query, suggestions, searchResults, includePartial);
    }
    
    // Entity-specific advanced search methods
    
    private List<Student> searchStudentsAdvanced(AdvancedSearchCriteria criteria) {
        return studentService.getAllStudents().stream()
                .filter(student -> matchesAdvancedCriteria(student, criteria))
                .filter(student -> matchesTextQuery(student, criteria.getQuery()))
                .limit(criteria.getMaxResults())
                .collect(Collectors.toList());
    }
    
    private List<Professor> searchProfessorsAdvanced(AdvancedSearchCriteria criteria) {
        return professorService.getAllProfessors().stream()
                .filter(professor -> matchesAdvancedCriteria(professor, criteria))
                .filter(professor -> matchesTextQuery(professor, criteria.getQuery()))
                .limit(criteria.getMaxResults())
                .collect(Collectors.toList());
    }
    
    private List<Course> searchCoursesAdvanced(AdvancedSearchCriteria criteria) {
        return courseService.getAllCourses().stream()
                .filter(course -> matchesAdvancedCriteria(course, criteria))
                .filter(course -> matchesTextQuery(course, criteria.getQuery()))
                .limit(criteria.getMaxResults())
                .collect(Collectors.toList());
    }
    
    private List<Department> searchDepartmentsAdvanced(AdvancedSearchCriteria criteria) {
        return departmentService.getAllDepartments().stream()
                .filter(department -> matchesAdvancedCriteria(department, criteria))
                .filter(department -> matchesTextQuery(department, criteria.getQuery()))
                .limit(criteria.getMaxResults())
                .collect(Collectors.toList());
    }
    
    private List<Enrollment> searchEnrollmentsAdvanced(AdvancedSearchCriteria criteria) {
        return enrollmentService.getEnrollmentsByStatus(Enrollment.EnrollmentStatus.ENROLLED).stream()
                .filter(enrollment -> matchesAdvancedCriteria(enrollment, criteria))
                .filter(enrollment -> matchesTextQuery(enrollment, criteria.getQuery()))
                .limit(criteria.getMaxResults())
                .collect(Collectors.toList());
    }
    
    private List<Grade> searchGradesAdvanced(AdvancedSearchCriteria criteria) {
        return gradeService.getAllGrades().stream()
                .filter(grade -> matchesAdvancedCriteria(grade, criteria))
                .filter(grade -> matchesTextQuery(grade, criteria.getQuery()))
                .limit(criteria.getMaxResults())
                .collect(Collectors.toList());
    }
    
    // Fuzzy search implementations
    
    private List<FuzzyMatch<Student>> fuzzySearchStudents(String query, double threshold) {
        return studentService.getAllStudents().stream()
                .map(student -> new FuzzyMatch<>(student, calculateSimilarity(query, getStudentSearchText(student))))
                .filter(match -> match.getScore() >= threshold)
                .sorted(Comparator.comparing(FuzzyMatch<Student>::getScore).reversed())
                .limit(maxSearchResults / 4)
                .collect(Collectors.toList());
    }
    
    private List<FuzzyMatch<Professor>> fuzzySearchProfessors(String query, double threshold) {
        return professorService.getAllProfessors().stream()
                .map(professor -> new FuzzyMatch<>(professor, calculateSimilarity(query, getProfessorSearchText(professor))))
                .filter(match -> match.getScore() >= threshold)
                .sorted(Comparator.comparing(FuzzyMatch<Professor>::getScore).reversed())
                .limit(maxSearchResults / 4)
                .collect(Collectors.toList());
    }
    
    private List<FuzzyMatch<Course>> fuzzySearchCourses(String query, double threshold) {
        return courseService.getAllCourses().stream()
                .map(course -> new FuzzyMatch<>(course, calculateSimilarity(query, getCourseSearchText(course))))
                .filter(match -> match.getScore() >= threshold)
                .sorted(Comparator.comparing(FuzzyMatch<Course>::getScore).reversed())
                .limit(maxSearchResults / 4)
                .collect(Collectors.toList());
    }
    
    private List<FuzzyMatch<Department>> fuzzySearchDepartments(String query, double threshold) {
        return departmentService.getAllDepartments().stream()
                .map(department -> new FuzzyMatch<>(department, calculateSimilarity(query, getDepartmentSearchText(department))))
                .filter(match -> match.getScore() >= threshold)
                .sorted(Comparator.comparing(FuzzyMatch<Department>::getScore).reversed())
                .limit(maxSearchResults / 4)
                .collect(Collectors.toList());
    }
    
    // Search utility methods
    
    private boolean matchesAdvancedCriteria(Object entity, AdvancedSearchCriteria criteria) {
        // Apply advanced filtering based on entity type and criteria
        if (entity instanceof Student) {
            return matchesStudentCriteria((Student) entity, criteria);
        } else if (entity instanceof Professor) {
            return matchesProfessorCriteria((Professor) entity, criteria);
        } else if (entity instanceof Course) {
            return matchesCourseCriteria((Course) entity, criteria);
        } else if (entity instanceof Department) {
            return matchesDepartmentCriteria((Department) entity, criteria);
        } else if (entity instanceof Enrollment) {
            return matchesEnrollmentCriteria((Enrollment) entity, criteria);
        } else if (entity instanceof Grade) {
            return matchesGradeCriteria((Grade) entity, criteria);
        }
        return true;
    }
    
    private boolean matchesTextQuery(Object entity, String query) {
        if (!ValidationUtil.isValidString(query)) {
            return true;
        }
        
        String searchText = getEntitySearchText(entity).toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        // Check for exact match, contains, or word match
        return searchText.contains(lowerQuery) || 
               Arrays.stream(searchText.split("\\s+"))
                     .anyMatch(word -> word.startsWith(lowerQuery));
    }
    
    private String getEntitySearchText(Object entity) {
        if (entity instanceof Student) {
            return getStudentSearchText((Student) entity);
        } else if (entity instanceof Professor) {
            return getProfessorSearchText((Professor) entity);
        } else if (entity instanceof Course) {
            return getCourseSearchText((Course) entity);
        } else if (entity instanceof Department) {
            return getDepartmentSearchText((Department) entity);
        } else if (entity instanceof Enrollment) {
            return getEnrollmentSearchText((Enrollment) entity);
        } else if (entity instanceof Grade) {
            return getGradeSearchText((Grade) entity);
        }
        return entity.toString();
    }
    
    private String getStudentSearchText(Student student) {
        return String.join(" ", 
            student.getFirstName(),
            student.getLastName(),
            student.getEmail(),
            student.getStudentId(),
            student.getMajor(),
            student.getAcademicYear().toString()
        );
    }
    
    private String getProfessorSearchText(Professor professor) {
        return String.join(" ",
            professor.getFirstName(),
            professor.getLastName(),
            professor.getEmail(),
            professor.getProfessorId(),
            professor.getDepartmentId(),
            professor.getAcademicRank().toString(),
            professor.getResearchArea() != null ? professor.getResearchArea() : ""
        );
    }
    
    private String getCourseSearchText(Course course) {
        return String.join(" ",
            course.getCourseName(),
            course.getCourseCode(),
            course.getCourseId(),
            course.getDescription(),
            course.getDepartmentId()
        );
    }
    
    private String getDepartmentSearchText(Department department) {
        return String.join(" ",
            department.getDepartmentName(),
            department.getDepartmentCode(),
            department.getDepartmentId(),
            department.getCollege(),
            department.getType().toString()
        );
    }
    
    private String getEnrollmentSearchText(Enrollment enrollment) {
        return String.join(" ",
            enrollment.getStudentId(),
            enrollment.getCourseId(),
            enrollment.getSemester(),
            String.valueOf(enrollment.getYear()),
            enrollment.getStatus().toString()
        );
    }
    
    private String getGradeSearchText(Grade grade) {
        return String.join(" ",
            grade.getStudentId(),
            grade.getCourseId(),
            grade.getAssignmentName(),
            grade.getComponent().toString(),
            grade.getStatus().toString(),
            grade.getLetterGrade() != null ? grade.getLetterGrade() : ""
        );
    }
    
    // Advanced criteria matching methods
    
    private boolean matchesStudentCriteria(Student student, AdvancedSearchCriteria criteria) {
        if (criteria.getMajor() != null && !criteria.getMajor().equals(student.getMajor())) {
            return false;
        }
        if (criteria.getAcademicYear() != null && !criteria.getAcademicYear().equals(student.getAcademicYear())) {
            return false;
        }
        if (criteria.getMinGpa() != null) {
            double gpa = studentService.calculateGPA(student.getStudentId());
            if (gpa < criteria.getMinGpa()) {
                return false;
            }
        }
        return true;
    }
    
    private boolean matchesProfessorCriteria(Professor professor, AdvancedSearchCriteria criteria) {
        if (criteria.getDepartmentId() != null && !criteria.getDepartmentId().equals(professor.getDepartmentId())) {
            return false;
        }
        if (criteria.getAcademicRank() != null && !criteria.getAcademicRank().equals(professor.getAcademicRank())) {
            return false;
        }
        if (criteria.getMinTeachingRating() != null && professor.getTeachingRating() < criteria.getMinTeachingRating()) {
            return false;
        }
        return true;
    }
    
    private boolean matchesCourseCriteria(Course course, AdvancedSearchCriteria criteria) {
        if (criteria.getDepartmentId() != null && !criteria.getDepartmentId().equals(course.getDepartmentId())) {
            return false;
        }
        if (criteria.getCourseStatus() != null && !criteria.getCourseStatus().equals(course.getStatus())) {
            return false;
        }
        if (criteria.getMinCreditHours() != null && course.getCreditHours() < criteria.getMinCreditHours()) {
            return false;
        }
        return true;
    }
    
    private boolean matchesDepartmentCriteria(Department department, AdvancedSearchCriteria criteria) {
        if (criteria.getCollege() != null && !criteria.getCollege().equals(department.getCollege())) {
            return false;
        }
        if (criteria.getDepartmentType() != null && !criteria.getDepartmentType().equals(department.getType())) {
            return false;
        }
        return true;
    }
    
    private boolean matchesEnrollmentCriteria(Enrollment enrollment, AdvancedSearchCriteria criteria) {
        if (criteria.getSemester() != null && !criteria.getSemester().equals(enrollment.getSemester())) {
            return false;
        }
        if (criteria.getYear() != null && !criteria.getYear().equals(enrollment.getYear())) {
            return false;
        }
        if (criteria.getEnrollmentStatus() != null && !criteria.getEnrollmentStatus().equals(enrollment.getStatus())) {
            return false;
        }
        return true;
    }
    
    private boolean matchesGradeCriteria(Grade grade, AdvancedSearchCriteria criteria) {
        if (criteria.getGradeComponent() != null && !criteria.getGradeComponent().equals(grade.getComponent())) {
            return false;
        }
        if (criteria.getMinGradePercentage() != null && grade.getPercentage() < criteria.getMinGradePercentage()) {
            return false;
        }
        return true;
    }
    
    // Search index and caching methods
    
    private void buildSearchIndex() {
        // Build search index for faster text searching
        // This is a simplified implementation
        
        // Index students
        studentService.getAllStudents().forEach(student -> {
            String searchText = getStudentSearchText(student);
            indexEntity("students", student.getStudentId(), searchText);
        });
        
        // Index professors
        professorService.getAllProfessors().forEach(professor -> {
            String searchText = getProfessorSearchText(professor);
            indexEntity("professors", professor.getProfessorId(), searchText);
        });
        
        // Index courses
        courseService.getAllCourses().forEach(course -> {
            String searchText = getCourseSearchText(course);
            indexEntity("courses", course.getCourseId(), searchText);
        });
        
        // Index departments
        departmentService.getAllDepartments().forEach(department -> {
            String searchText = getDepartmentSearchText(department);
            indexEntity("departments", department.getDepartmentId(), searchText);
        });
    }
    
    private void indexEntity(String entityType, String entityId, String searchText) {
        String[] words = searchText.toLowerCase().split("\\s+");
        for (String word : words) {
            String key = entityType + ":" + word;
            searchIndex.computeIfAbsent(key, k -> new HashSet<>()).add(entityId);
        }
    }
    
    private Map<Class<?>, Searchable<?>> initializeSearchableServices() {
        Map<Class<?>, Searchable<?>> services = new HashMap<>();
        services.put(Student.class, studentService);
        services.put(Professor.class, professorService);
        services.put(Course.class, courseService);
        services.put(Department.class, departmentService);
        services.put(Enrollment.class, enrollmentService);
        services.put(Grade.class, gradeService);
        return services;
    }
    
    // Similarity calculation for fuzzy search
    
    private double calculateSimilarity(String query, String text) {
        if (query == null || text == null) {
            return 0.0;
        }
        
        String lowerQuery = query.toLowerCase();
        String lowerText = text.toLowerCase();
        
        // Simple similarity calculation using Levenshtein distance
        int distance = calculateLevenshteinDistance(lowerQuery, lowerText);
        int maxLength = Math.max(lowerQuery.length(), lowerText.length());
        
        return maxLength > 0 ? 1.0 - (double) distance / maxLength : 0.0;
    }
    
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    // Utility methods for search results
    
    private Map<String, Double> calculateRelevanceScores(Map<String, List<?>> results, AdvancedSearchCriteria criteria) {
        Map<String, Double> scores = new HashMap<>();
        
        for (Map.Entry<String, List<?>> entry : results.entrySet()) {
            String entityType = entry.getKey();
            List<?> entities = entry.getValue();
            
            double score = calculateEntityTypeRelevance(entityType, entities, criteria);
            scores.put(entityType, score);
        }
        
        return scores;
    }
    
    private double calculateEntityTypeRelevance(String entityType, List<?> entities, AdvancedSearchCriteria criteria) {
        if (entities.isEmpty()) {
            return 0.0;
        }
        
        // Simple relevance scoring based on result count and query specificity
        double baseScore = Math.min(1.0, entities.size() / 10.0);
        double specificityBonus = criteria.getQuery() != null ? criteria.getQuery().length() / 50.0 : 0.0;
        
        return Math.min(1.0, baseScore + specificityBonus);
    }
    
    private List<?> applyFacetFilters(List<?> entities, Set<String> facetValues) {
        if (facetValues.isEmpty()) {
            return entities;
        }
        
        // Simple facet filtering - this would be more sophisticated in a real implementation
        return entities.stream()
                .filter(entity -> facetValues.contains(getEntityFacetValue(entity)))
                .collect(Collectors.toList());
    }
    
    private String getEntityFacetValue(Object entity) {
        if (entity instanceof Student) {
            return ((Student) entity).getMajor();
        } else if (entity instanceof Professor) {
            return ((Professor) entity).getDepartmentId();
        } else if (entity instanceof Course) {
            return ((Course) entity).getDepartmentId();
        } else if (entity instanceof Department) {
            return ((Department) entity).getCollege();
        }
        return "unknown";
    }
    
    private Map<String, Long> calculateFacetCounts(List<?> entities, String entityType) {
        return entities.stream()
                .map(this::getEntityFacetValue)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
    
    private List<SearchSuggestion> getServiceSuggestions(String entityType, Searchable<?> service, 
                                                        String partialQuery, int maxSuggestions) {
        return service.getSearchSuggestions(partialQuery, maxSuggestions).stream()
                .map(suggestion -> new SearchSuggestion(suggestion, entityType, calculateSuggestionScore(suggestion, partialQuery)))
                .collect(Collectors.toList());
    }
    
    private double calculateSuggestionScore(String suggestion, String partialQuery) {
        if (suggestion == null || partialQuery == null) {
            return 0.0;
        }
        
        String lowerSuggestion = suggestion.toLowerCase();
        String lowerQuery = partialQuery.toLowerCase();
        
        // Higher score for exact prefix matches
        if (lowerSuggestion.startsWith(lowerQuery)) {
            return 1.0;
        }
        
        // Medium score for contains matches
        if (lowerSuggestion.contains(lowerQuery)) {
            return 0.7;
        }
        
        // Lower score for fuzzy matches
        return calculateSimilarity(lowerQuery, lowerSuggestion);
    }
    
    // Clear search cache
    public void clearSearchCache() {
        searchCache.clear();
    }
    
    // Rebuild search index
    public void rebuildSearchIndex() {
        searchIndex.clear();
        buildSearchIndex();
    }
    
    // Search result classes and records
    
    /**
     * Universal search result containing results from all entity types.
     */
    public static class UniversalSearchResult {
        private final String query;
        private final Map<String, List<?>> results;
        private final int totalResults;
        
        public UniversalSearchResult(String query, Map<String, List<?>> results) {
            this.query = query;
            this.results = new HashMap<>(results);
            this.totalResults = results.values().stream()
                    .mapToInt(List::size)
                    .sum();
        }
        
        public String getQuery() { return query; }
        public Map<String, List<?>> getResults() { return results; }
        public int getTotalResults() { return totalResults; }
        
        @SuppressWarnings("unchecked")
        public <T> List<T> getResultsForType(String entityType, Class<T> type) {
            List<?> entityResults = results.get(entityType);
            if (entityResults == null) {
                return Collections.emptyList();
            }
            
            return entityResults.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .collect(Collectors.toList());
        }
        
        public boolean hasResults() {
            return totalResults > 0;
        }
        
        @Override
        public String toString() {
            return String.format("UniversalSearchResult{query='%s', totalResults=%d, entityTypes=%s}",
                    query, totalResults, results.keySet());
        }
    }
    
    /**
     * Advanced search result with relevance scoring.
     */
    public static class AdvancedSearchResult {
        private final AdvancedSearchCriteria criteria;
        private final Map<String, List<?>> results;
        private final Map<String, Double> relevanceScores;
        private final int totalResults;
        
        public AdvancedSearchResult(AdvancedSearchCriteria criteria, Map<String, List<?>> results, 
                                  Map<String, Double> relevanceScores) {
            this.criteria = criteria;
            this.results = new HashMap<>(results);
            this.relevanceScores = new HashMap<>(relevanceScores);
            this.totalResults = results.values().stream()
                    .mapToInt(List::size)
                    .sum();
        }
        
        public AdvancedSearchCriteria getCriteria() { return criteria; }
        public Map<String, List<?>> getResults() { return results; }
        public Map<String, Double> getRelevanceScores() { return relevanceScores; }
        public int getTotalResults() { return totalResults; }
        
        public double getRelevanceScore(String entityType) {
            return relevanceScores.getOrDefault(entityType, 0.0);
        }
        
        @Override
        public String toString() {
            return String.format("AdvancedSearchResult{totalResults=%d, avgRelevance=%.2f}",
                    totalResults, relevanceScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        }
    }
    
    /**
     * Fuzzy search result with similarity scores.
     */
    public static class FuzzySearchResult {
        private final String query;
        private final double threshold;
        private final Map<String, List<FuzzyMatch<?>>> results;
        private final int totalMatches;
        
        public FuzzySearchResult(String query, double threshold, Map<String, List<FuzzyMatch<?>>> results) {
            this.query = query;
            this.threshold = threshold;
            this.results = new HashMap<>(results);
            this.totalMatches = results.values().stream()
                    .mapToInt(List::size)
                    .sum();
        }
        
        public String getQuery() { return query; }
        public double getThreshold() { return threshold; }
        public Map<String, List<FuzzyMatch<?>>> getResults() { return results; }
        public int getTotalMatches() { return totalMatches; }
        
        public double getAverageScore() {
            return results.values().stream()
                    .flatMap(List::stream)
                    .mapToDouble(FuzzyMatch::getScore)
                    .average()
                    .orElse(0.0);
        }
        
        @Override
        public String toString() {
            return String.format("FuzzySearchResult{query='%s', threshold=%.2f, totalMatches=%d, avgScore=%.2f}",
                    query, threshold, totalMatches, getAverageScore());
        }
    }
    
    /**
     * Faceted search result with facet counts.
     */
    public static class FacetedSearchResult {
        private final String query;
        private final Map<String, Set<String>> appliedFacets;
        private final Map<String, List<?>> results;
        private final Map<String, Map<String, Long>> facetCounts;
        private final int totalResults;
        
        public FacetedSearchResult(String query, Map<String, Set<String>> appliedFacets,
                                 Map<String, List<?>> results, Map<String, Map<String, Long>> facetCounts) {
            this.query = query;
            this.appliedFacets = new HashMap<>(appliedFacets);
            this.results = new HashMap<>(results);
            this.facetCounts = new HashMap<>(facetCounts);
            this.totalResults = results.values().stream()
                    .mapToInt(List::size)
                    .sum();
        }
        
        public String getQuery() { return query; }
        public Map<String, Set<String>> getAppliedFacets() { return appliedFacets; }
        public Map<String, List<?>> getResults() { return results; }
        public Map<String, Map<String, Long>> getFacetCounts() { return facetCounts; }
        public int getTotalResults() { return totalResults; }
        
        public Map<String, Long> getFacetCountsForType(String entityType) {
            return facetCounts.getOrDefault(entityType, Collections.emptyMap());
        }
        
        @Override
        public String toString() {
            return String.format("FacetedSearchResult{query='%s', totalResults=%d, facetTypes=%d}",
                    query, totalResults, facetCounts.size());
        }
    }
    
    /**
     * Auto-complete result with suggestions and search results.
     */
    public static class AutoCompleteResult {
        private final String query;
        private final List<SearchSuggestion> suggestions;
        private final UniversalSearchResult searchResults;
        private final boolean includePartial;
        
        public AutoCompleteResult(String query, List<SearchSuggestion> suggestions,
                                UniversalSearchResult searchResults, boolean includePartial) {
            this.query = query;
            this.suggestions = new ArrayList<>(suggestions);
            this.searchResults = searchResults;
            this.includePartial = includePartial;
        }
        
        public String getQuery() { return query; }
        public List<SearchSuggestion> getSuggestions() { return suggestions; }
        public UniversalSearchResult getSearchResults() { return searchResults; }
        public boolean isIncludePartial() { return includePartial; }
        
        public boolean hasSearchResults() {
            return searchResults != null && searchResults.hasResults();
        }
        
        public boolean hasSuggestions() {
            return !suggestions.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("AutoCompleteResult{query='%s', suggestions=%d, hasResults=%s}",
                    query, suggestions.size(), hasSearchResults());
        }
    }
    
    /**
     * Fuzzy match with similarity score.
     */
    public static class FuzzyMatch<T> {
        private final T entity;
        private final double score;
        
        public FuzzyMatch(T entity, double score) {
            this.entity = entity;
            this.score = score;
        }
        
        public T getEntity() { return entity; }
        public double getScore() { return score; }
        
        @Override
        public String toString() {
            return String.format("FuzzyMatch{entity=%s, score=%.3f}", entity, score);
        }
    }
    
    /**
     * Search suggestion with scoring.
     */
    public static class SearchSuggestion {
        private final String suggestion;
        private final String entityType;
        private final double score;
        
        public SearchSuggestion(String suggestion, String entityType, double score) {
            this.suggestion = suggestion;
            this.entityType = entityType;
            this.score = score;
        }
        
        public String getSuggestion() { return suggestion; }
        public String getEntityType() { return entityType; }
        public double getScore() { return score; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchSuggestion that = (SearchSuggestion) o;
            return Objects.equals(suggestion, that.suggestion) && Objects.equals(entityType, that.entityType);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(suggestion, entityType);
        }
        
        @Override
        public String toString() {
            return String.format("SearchSuggestion{suggestion='%s', type='%s', score=%.3f}", 
                    suggestion, entityType, score);
        }
    }
    
    /**
     * Advanced search criteria for complex queries.
     */
    public static class AdvancedSearchCriteria {
        private String query;
        private boolean includeStudents = true;
        private boolean includeProfessors = true;
        private boolean includeCourses = true;
        private boolean includeDepartments = true;
        private boolean includeEnrollments = true;
        private boolean includeGrades = true;
        private int maxResults = 100;
        
        // Student-specific criteria
        private String major;
        private Student.AcademicYear academicYear;
        private Double minGpa;
        
        // Professor-specific criteria
        private String departmentId;
        private Professor.AcademicRank academicRank;
        private Double minTeachingRating;
        
        // Course-specific criteria
        private Course.CourseStatus courseStatus;
        private Integer minCreditHours;
        
        // Department-specific criteria
        private String college;
        private Department.DepartmentType departmentType;
        
        // Enrollment-specific criteria
        private String semester;
        private Integer year;
        private Enrollment.EnrollmentStatus enrollmentStatus;
        
        // Grade-specific criteria
        private Grade.GradeComponent gradeComponent;
        private Double minGradePercentage;
        
        // Constructors
        public AdvancedSearchCriteria() {}
        
        public AdvancedSearchCriteria(String query) {
            this.query = query;
        }
        
        // Builder pattern methods
        public AdvancedSearchCriteria withQuery(String query) {
            this.query = query;
            return this;
        }
        
        public AdvancedSearchCriteria withMaxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }
        
        public AdvancedSearchCriteria withMajor(String major) {
            this.major = major;
            return this;
        }
        
        public AdvancedSearchCriteria withAcademicYear(Student.AcademicYear academicYear) {
            this.academicYear = academicYear;
            return this;
        }
        
        public AdvancedSearchCriteria withMinGpa(Double minGpa) {
            this.minGpa = minGpa;
            return this;
        }
        
        public AdvancedSearchCriteria withDepartmentId(String departmentId) {
            this.departmentId = departmentId;
            return this;
        }
        
        public AdvancedSearchCriteria withAcademicRank(Professor.AcademicRank academicRank) {
            this.academicRank = academicRank;
            return this;
        }
        
        public AdvancedSearchCriteria withMinTeachingRating(Double minTeachingRating) {
            this.minTeachingRating = minTeachingRating;
            return this;
        }
        
        public AdvancedSearchCriteria withCourseStatus(Course.CourseStatus courseStatus) {
            this.courseStatus = courseStatus;
            return this;
        }
        
        public AdvancedSearchCriteria withMinCreditHours(Integer minCreditHours) {
            this.minCreditHours = minCreditHours;
            return this;
        }
        
        public AdvancedSearchCriteria withCollege(String college) {
            this.college = college;
            return this;
        }
        
        public AdvancedSearchCriteria withDepartmentType(Department.DepartmentType departmentType) {
            this.departmentType = departmentType;
            return this;
        }
        
        public AdvancedSearchCriteria withSemester(String semester) {
            this.semester = semester;
            return this;
        }
        
        public AdvancedSearchCriteria withYear(Integer year) {
            this.year = year;
            return this;
        }
        
        public AdvancedSearchCriteria withEnrollmentStatus(Enrollment.EnrollmentStatus enrollmentStatus) {
            this.enrollmentStatus = enrollmentStatus;
            return this;
        }
        
        public AdvancedSearchCriteria withGradeComponent(Grade.GradeComponent gradeComponent) {
            this.gradeComponent = gradeComponent;
            return this;
        }
        
        public AdvancedSearchCriteria withMinGradePercentage(Double minGradePercentage) {
            this.minGradePercentage = minGradePercentage;
            return this;
        }
        
        public AdvancedSearchCriteria excludeStudents() {
            this.includeStudents = false;
            return this;
        }
        
        public AdvancedSearchCriteria excludeProfessors() {
            this.includeProfessors = false;
            return this;
        }
        
        public AdvancedSearchCriteria excludeCourses() {
            this.includeCourses = false;
            return this;
        }
        
        public AdvancedSearchCriteria excludeDepartments() {
            this.includeDepartments = false;
            return this;
        }
        
        public AdvancedSearchCriteria excludeEnrollments() {
            this.includeEnrollments = false;
            return this;
        }
        
        public AdvancedSearchCriteria excludeGrades() {
            this.includeGrades = false;
            return this;
        }
        
        // Getters
        public String getQuery() { return query; }
        public boolean includeStudents() { return includeStudents; }
        public boolean includeProfessors() { return includeProfessors; }
        public boolean includeCourses() { return includeCourses; }
        public boolean includeDepartments() { return includeDepartments; }
        public boolean includeEnrollments() { return includeEnrollments; }
        public boolean includeGrades() { return includeGrades; }
        public int getMaxResults() { return maxResults; }
        
        public String getMajor() { return major; }
        public Student.AcademicYear getAcademicYear() { return academicYear; }
        public Double getMinGpa() { return minGpa; }
        public String getDepartmentId() { return departmentId; }
        public Professor.AcademicRank getAcademicRank() { return academicRank; }
        public Double getMinTeachingRating() { return minTeachingRating; }
        public Course.CourseStatus getCourseStatus() { return courseStatus; }
        public Integer getMinCreditHours() { return minCreditHours; }
        public String getCollege() { return college; }
        public Department.DepartmentType getDepartmentType() { return departmentType; }
        public String getSemester() { return semester; }
        public Integer getYear() { return year; }
        public Enrollment.EnrollmentStatus getEnrollmentStatus() { return enrollmentStatus; }
        public Grade.GradeComponent getGradeComponent() { return gradeComponent; }
        public Double getMinGradePercentage() { return minGradePercentage; }
        
        @Override
        public String toString() {
            return String.format("AdvancedSearchCriteria{query='%s', maxResults=%d, entities=[%s%s%s%s%s%s]}",
                    query, maxResults,
                    includeStudents ? "Students," : "",
                    includeProfessors ? "Professors," : "",
                    includeCourses ? "Courses," : "",
                    includeDepartments ? "Departments," : "",
                    includeEnrollments ? "Enrollments," : "",
                    includeGrades ? "Grades" : "");
        }
    }
}