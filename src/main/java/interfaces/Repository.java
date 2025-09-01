// File: src/main/java/interfaces/Repository.java
package interfaces;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Repository interface defining generic data access operations.
 * This interface provides a contract for data access layer implementations.
 * 
 * Key Java concepts demonstrated:
 * - Generic interface definition
 * - Optional return types (Java 8+)
 * - Functional interfaces and Predicate
 * - Method overloading
 * - Default methods with implementations
 * - Exception handling in method signatures
 */
public interface Repository<T, ID> {
    
    /**
     * Save an entity.
     * 
     * @param entity The entity to save
     * @return The saved entity
     * @throws RepositoryException if save operation fails
     */
    T save(T entity) throws RepositoryException;
    
    /**
     * Save multiple entities in batch.
     * 
     * @param entities List of entities to save
     * @return List of saved entities
     * @throws RepositoryException if batch save operation fails
     */
    List<T> saveAll(List<T> entities) throws RepositoryException;
    
    /**
     * Find an entity by its ID.
     * 
     * @param id The ID of the entity
     * @return Optional containing the entity if found, empty otherwise
     * @throws RepositoryException if find operation fails
     */
    Optional<T> findById(ID id) throws RepositoryException;
    
    /**
     * Check if an entity exists by ID.
     * 
     * @param id The ID to check
     * @return true if entity exists, false otherwise
     * @throws RepositoryException if existence check fails
     */
    boolean existsById(ID id) throws RepositoryException;
    
    /**
     * Find all entities.
     * 
     * @return List of all entities
     * @throws RepositoryException if find operation fails
     */
    List<T> findAll() throws RepositoryException;
    
    /**
     * Find entities by a list of IDs.
     * 
     * @param ids List of IDs to search for
     * @return List of found entities
     * @throws RepositoryException if find operation fails
     */
    List<T> findAllById(List<ID> ids) throws RepositoryException;
    
    /**
     * Find entities matching a predicate.
     * 
     * @param predicate The predicate to match
     * @return List of matching entities
     * @throws RepositoryException if find operation fails
     */
    List<T> findByPredicate(Predicate<T> predicate) throws RepositoryException;
    
    /**
     * Find the first entity matching a predicate.
     * 
     * @param predicate The predicate to match
     * @return Optional containing the first matching entity, empty if none found
     * @throws RepositoryException if find operation fails
     */
    Optional<T> findFirstByPredicate(Predicate<T> predicate) throws RepositoryException;
    
    /**
     * Count total number of entities.
     * 
     * @return Total count of entities
     * @throws RepositoryException if count operation fails
     */
    long count() throws RepositoryException;
    
    /**
     * Count entities matching a predicate.
     * 
     * @param predicate The predicate to match
     * @return Count of matching entities
     * @throws RepositoryException if count operation fails
     */
    long countByPredicate(Predicate<T> predicate) throws RepositoryException;
    
    /**
     * Update an existing entity.
     * 
     * @param entity The entity to update
     * @return The updated entity
     * @throws RepositoryException if update operation fails
     */
    T update(T entity) throws RepositoryException;
    
    /**
     * Update multiple entities in batch.
     * 
     * @param entities List of entities to update
     * @return List of updated entities
     * @throws RepositoryException if batch update operation fails
     */
    List<T> updateAll(List<T> entities) throws RepositoryException;
    
    /**
     * Delete an entity by ID.
     * 
     * @param id The ID of the entity to delete
     * @throws RepositoryException if delete operation fails
     */
    void deleteById(ID id) throws RepositoryException;
    
    /**
     * Delete an entity.
     * 
     * @param entity The entity to delete
     * @throws RepositoryException if delete operation fails
     */
    void delete(T entity) throws RepositoryException;
    
    /**
     * Delete multiple entities by IDs.
     * 
     * @param ids List of IDs to delete
     * @throws RepositoryException if batch delete operation fails
     */
    void deleteAllById(List<ID> ids) throws RepositoryException;
    
    /**
     * Delete multiple entities.
     * 
     * @param entities List of entities to delete
     * @throws RepositoryException if batch delete operation fails
     */
    void deleteAll(List<T> entities) throws RepositoryException;
    
    /**
     * Delete all entities.
     * 
     * @throws RepositoryException if delete all operation fails
     */
    void deleteAll() throws RepositoryException;
    
    /**
     * Delete entities matching a predicate.
     * 
     * @param predicate The predicate to match for deletion
     * @return Number of entities deleted
     * @throws RepositoryException if delete operation fails
     */
    int deleteByPredicate(Predicate<T> predicate) throws RepositoryException;
    
    /**
     * Find entities with pagination.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of entities
     * @throws RepositoryException if find operation fails
     */
    Page<T> findAll(int page, int size) throws RepositoryException;
    
    /**
     * Find entities matching predicate with pagination.
     * 
     * @param predicate The predicate to match
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of matching entities
     * @throws RepositoryException if find operation fails
     */
    Page<T> findByPredicate(Predicate<T> predicate, int page, int size) throws RepositoryException;
    
    /**
     * Refresh an entity from the data source.
     * 
     * @param entity The entity to refresh
     * @return The refreshed entity
     * @throws RepositoryException if refresh operation fails
     */
    T refresh(T entity) throws RepositoryException;
    
    /**
     * Flush pending changes to the data source.
     * 
     * @throws RepositoryException if flush operation fails
     */
    void flush() throws RepositoryException;
    
    /**
     * Save and flush an entity.
     * 
     * @param entity The entity to save and flush
     * @return The saved entity
     * @throws RepositoryException if save and flush operation fails
     */
    default T saveAndFlush(T entity) throws RepositoryException {
        T saved = save(entity);
        flush();
        return saved;
    }
    
    /**
     * Default method to check if repository is empty.
     * 
     * @return true if repository is empty, false otherwise
     * @throws RepositoryException if count operation fails
     */
    default boolean isEmpty() throws RepositoryException {
        return count() == 0;
    }
    
    /**
     * Default method to get first entity.
     * 
     * @return Optional containing the first entity, empty if none found
     * @throws RepositoryException if find operation fails
     */
    default Optional<T> findFirst() throws RepositoryException {
        Page<T> page = findAll(0, 1);
        return page.getContent().stream().findFirst();
    }
    
    /**
     * Default method to get last entity (assuming natural ordering).
     * 
     * @return Optional containing the last entity, empty if none found
     * @throws RepositoryException if find operation fails
     */
    default Optional<T> findLast() throws RepositoryException {
        long totalCount = count();
        if (totalCount == 0) {
            return Optional.empty();
        }
        Page<T> page = findAll((int) totalCount - 1, 1);
        return page.getContent().stream().findFirst();
    }
    
    /**
     * Inner class representing a page of results.
     */
    class Page<T> {
        private final List<T> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;
        private final boolean hasNext;
        private final boolean hasPrevious;
        
        public Page(List<T> content, int page, int size, long totalElements) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / size);
            this.first = page == 0;
            this.last = page >= totalPages - 1;
            this.hasNext = page < totalPages - 1;
            this.hasPrevious = page > 0;
        }
        
        // Getters
        public List<T> getContent() { return content; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public boolean isFirst() { return first; }
        public boolean isLast() { return last; }
        public boolean hasNext() { return hasNext; }
        public boolean hasPrevious() { return hasPrevious; }
        public int getNumberOfElements() { return content.size(); }
        public boolean isEmpty() { return content.isEmpty(); }
        
        @Override
        public String toString() {
            return String.format("Page{page=%d, size=%d, totalElements=%d, totalPages=%d, content=%d items}",
                page, size, totalElements, totalPages, content.size());
        }
    }
    
    /**
     * Custom exception for repository operations.
     */
    class RepositoryException extends Exception {
        private final String operation;
        private final Object entityId;
        
        public RepositoryException(String message) {
            super(message);
            this.operation = null;
            this.entityId = null;
        }
        
        public RepositoryException(String message, Throwable cause) {
            super(message, cause);
            this.operation = null;
            this.entityId = null;
        }
        
        public RepositoryException(String operation, Object entityId, String message) {
            super(String.format("Repository operation '%s' failed for entity '%s': %s", operation, entityId, message));
            this.operation = operation;
            this.entityId = entityId;
        }
        
        public RepositoryException(String operation, Object entityId, String message, Throwable cause) {
            super(String.format("Repository operation '%s' failed for entity '%s': %s", operation, entityId, message), cause);
            this.operation = operation;
            this.entityId = entityId;
        }
        
        public String getOperation() { return operation; }
        public Object getEntityId() { return entityId; }
    }
}