// File: src/main/java/interfaces/Searchable.java
package interfaces;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Searchable interface defining search and filtering operations.
 * This interface provides a contract for entities that support search functionality.
 * 
 * Key Java concepts demonstrated:
 * - Interface definition with generic types
 * - Functional interfaces and Predicate (Java 8+)
 * - Default methods with implementation
 * - Static methods in interfaces
 * - Nested classes and enums
 * - Method overloading
 */
public interface Searchable<T> {
    
    /**
     * Search criteria enumeration.
     */
    enum SearchCriteria {
        EXACT_MATCH("Exact Match"),
        CONTAINS("Contains"),
        STARTS_WITH("Starts With"),
        ENDS_WITH("Ends With"),
        REGEX("Regular Expression"),
        FUZZY("Fuzzy Match"),
        RANGE("Range"),
        GREATER_THAN("Greater Than"),
        LESS_THAN("Less Than"),
        BETWEEN("Between");
        
        private final String displayName;
        
        SearchCriteria(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Sort order enumeration.
     */
    enum SortOrder {
        ASC("Ascending"),
        DESC("Descending");
        
        private final String displayName;
        
        SortOrder(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Basic search by keyword.
     * 
     * @param keyword The keyword to search for
     * @return List of matching entities
     */
    List<T> search(String keyword);
    
    /**
     * Advanced search with multiple criteria.
     * 
     * @param searchCriteria Map of field names to search criteria and values
     * @return List of matching entities
     */
    List<T> search(Map<String, SearchCriterion> searchCriteria);
    
    /**
     * Search with custom predicate.
     * 
     * @param predicate Custom predicate for filtering
     * @return List of matching entities
     */
    List<T> search(Predicate<T> predicate);
    
    /**
     * Search and sort results.
     * 
     * @param keyword The keyword to search for
     * @param sortBy Field to sort by
     * @param sortOrder Sort order (ASC or DESC)
     * @return List of matching entities sorted by specified criteria
     */
    List<T> searchAndSort(String keyword, String sortBy, SortOrder sortOrder);
    
    /**
     * Search with pagination.
     * 
     * @param keyword The keyword to search for
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return SearchResult containing paginated results
     */
    SearchResult<T> searchWithPagination(String keyword, int page, int pageSize);
    
    /**
     * Advanced search with pagination and sorting.
     * 
     * @param searchCriteria Map of search criteria
     * @param sortBy Field to sort by
     * @param sortOrder Sort order
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return SearchResult containing paginated and sorted results
     */
    SearchResult<T> advancedSearchWithPagination(Map<String, SearchCriterion> searchCriteria, 
                                                String sortBy, SortOrder sortOrder, 
                                                int page, int pageSize);
    
    /**
     * Get search suggestions based on partial input.
     * 
     * @param partialInput Partial input from user
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of search suggestions
     */
    List<String> getSearchSuggestions(String partialInput, int maxSuggestions);
    
    /**
     * Filter entities based on a predicate.
     * 
     * @param predicate Filter predicate
     * @return List of filtered entities
     */
    List<T> filter(Predicate<T> predicate);
    
    /**
     * Get all searchable fields for this entity type.
     * 
     * @return List of field names that can be searched
     */
    List<String> getSearchableFields();
    
    /**
     * Get all sortable fields for this entity type.
     * 
     * @return List of field names that can be used for sorting
     */
    List<String> getSortableFields();
    
    /**
     * Count total number of search results without returning the actual results.
     * 
     * @param keyword The keyword to search for
     * @return Number of matching results
     */
    long countSearchResults(String keyword);
    
    /**
     * Count search results for advanced criteria.
     * 
     * @param searchCriteria Map of search criteria
     * @return Number of matching results
     */
    long countSearchResults(Map<String, SearchCriterion> searchCriteria);
    
    /**
     * Default method for case-insensitive keyword search.
     * 
     * @param keyword The keyword to search for
     * @return List of matching entities (case-insensitive)
     */
    default List<T> searchIgnoreCase(String keyword) {
        return search(keyword.toLowerCase());
    }
    
    /**
     * Default method to search multiple keywords with AND logic.
     * 
     * @param keywords List of keywords that must all match
     * @return List of entities matching all keywords
     */
    default List<T> searchMultipleKeywords(List<String> keywords) {
        Predicate<T> combinedPredicate = null;
        for (String keyword : keywords) {
            Predicate<T> keywordPredicate = entity -> 
                search(keyword).contains(entity);
            if (combinedPredicate == null) {
                combinedPredicate = keywordPredicate;
            } else {
                combinedPredicate = combinedPredicate.and(keywordPredicate);
            }
        }
        return combinedPredicate != null ? filter(combinedPredicate) : List.of();
    }
    
    /**
     * Static utility method to create a basic search criterion.
     * 
     * @param criteria The search criteria type
     * @param value The value to search for
     * @return SearchCriterion instance
     */
    static SearchCriterion createCriterion(SearchCriteria criteria, Object value) {
        return new SearchCriterion(criteria, value);
    }
    
    /**
     * Static utility method to create a range search criterion.
     * 
     * @param minValue The minimum value in the range
     * @param maxValue The maximum value in the range
     * @return SearchCriterion instance for range search
     */
    static SearchCriterion createRangeCriterion(Object minValue, Object maxValue) {
        return new SearchCriterion(SearchCriteria.RANGE, minValue, maxValue);
    }
    
    /**
     * Inner class representing a search criterion.
     */
    class SearchCriterion {
        private final SearchCriteria criteria;
        private final Object value;
        private final Object secondaryValue; // For range queries
        private final boolean caseSensitive;
        
        public SearchCriterion(SearchCriteria criteria, Object value) {
            this.criteria = criteria;
            this.value = value;
            this.secondaryValue = null;
            this.caseSensitive = false;
        }
        
        public SearchCriterion(SearchCriteria criteria, Object value, boolean caseSensitive) {
            this.criteria = criteria;
            this.value = value;
            this.secondaryValue = null;
            this.caseSensitive = caseSensitive;
        }
        
        public SearchCriterion(SearchCriteria criteria, Object value, Object secondaryValue) {
            this.criteria = criteria;
            this.value = value;
            this.secondaryValue = secondaryValue;
            this.caseSensitive = false;
        }
        
        // Getters
        public SearchCriteria getCriteria() { return criteria; }
        public Object getValue() { return value; }
        public Object getSecondaryValue() { return secondaryValue; }
        public boolean isCaseSensitive() { return caseSensitive; }
        
        @Override
        public String toString() {
            if (secondaryValue != null) {
                return String.format("SearchCriterion{criteria=%s, value=%s, secondaryValue=%s}",
                    criteria, value, secondaryValue);
            }
            return String.format("SearchCriterion{criteria=%s, value=%s, caseSensitive=%s}",
                criteria, value, caseSensitive);
        }
    }
    
    /**
     * Inner class representing search results with pagination information.
     */
    class SearchResult<T> {
        private final List<T> results;
        private final int page;
        private final int pageSize;
        private final long totalElements;
        private final int totalPages;
        private final boolean hasNext;
        private final boolean hasPrevious;
        private final String sortBy;
        private final SortOrder sortOrder;
        
        public SearchResult(List<T> results, int page, int pageSize, long totalElements) {
            this.results = results;
            this.page = page;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
            this.hasNext = page < totalPages - 1;
            this.hasPrevious = page > 0;
            this.sortBy = null;
            this.sortOrder = null;
        }
        
        public SearchResult(List<T> results, int page, int pageSize, long totalElements, 
                           String sortBy, SortOrder sortOrder) {
            this(results, page, pageSize, totalElements);
            this.sortBy = sortBy;
            this.sortOrder = sortOrder;
        }
        
        // Getters
        public List<T> getResults() { return results; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public boolean hasNext() { return hasNext; }
        public boolean hasPrevious() { return hasPrevious; }
        public String getSortBy() { return sortBy; }
        public SortOrder getSortOrder() { return sortOrder; }
        public int getResultCount() { return results.size(); }
        
        public boolean isEmpty() { return results.isEmpty(); }
        public boolean isFirstPage() { return page == 0; }
        public boolean isLastPage() { return page == totalPages - 1; }
        
        @Override
        public String toString() {
            return String.format("SearchResult{page=%d/%d, size=%d, total=%d, hasNext=%s, hasPrevious=%s}",
                page + 1, totalPages, results.size(), totalElements, hasNext, hasPrevious);
        }
    }
}