// File: src/main/java/interfaces/CrudOperations.java
package interfaces;

import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * CrudOperations interface defining basic Create, Read, Update, Delete operations.
 * This interface provides a simplified contract for basic data operations.
 * 
 * Key Java concepts demonstrated:
 * - Generic interface with type parameters
 * - Optional return types for safe null handling
 * - Method overloading for different parameter sets
 * - Default methods with implementations
 * - Exception handling in interface methods
 */
public interface CrudOperations<T, ID> {
    
    // CREATE operations
    
    /**
     * Create a new entity.
     * 
     * @param entity The entity to create
     * @return The created entity with generated ID
     * @throws CrudException if creation fails
     */
    T create(T entity) throws CrudException;
    
    /**
     * Create multiple entities in batch.
     * 
     * @param entities List of entities to create
     * @return List of created entities with generated IDs
     * @throws CrudException if batch creation fails
     */
    List<T> createAll(List<T> entities) throws CrudException;
    
    /**
     * Create entity with specific ID.
     * 
     * @param id The ID to assign to the entity
     * @param entity The entity to create
     * @return The created entity
     * @throws CrudException if creation fails or ID already exists
     */
    T createWithId(ID id, T entity) throws CrudException;
    
    // READ operations
    
    /**
     * Read an entity by ID.
     * 
     * @param id The ID of the entity to read
     * @return Optional containing the entity if found, empty otherwise
     * @throws CrudException if read operation fails
     */
    Optional<T> read(ID id) throws CrudException;
    
    /**
     * Read all entities.
     * 
     * @return List of all entities
     * @throws CrudException if read operation fails
     */
    List<T> readAll() throws CrudException;
    
    /**
     * Read entities by multiple IDs.
     * 
     * @param ids List of IDs to read
     * @return List of found entities
     * @throws CrudException if read operation fails
     */
    List<T> readByIds(List<ID> ids) throws CrudException;
    
    /**
     * Read entities with pagination.
     * 
     * @param offset Starting position (0-based)
     * @param limit Maximum number of entities to return
     * @return List of entities within the specified range
     * @throws CrudException if read operation fails
     */
    List<T> readWithPagination(int offset, int limit) throws CrudException;
    
    /**
     * Read entities by field value.
     * 
     * @param fieldName The name of the field to search by
     * @param value The value to search for
     * @return List of entities matching the criteria
     * @throws CrudException if read operation fails
     */
    List<T> readByField(String fieldName, Object value) throws CrudException;
    
    /**
     * Read entities by multiple field criteria.
     * 
     * @param criteria Map of field names to values
     * @return List of entities matching all criteria
     * @throws CrudException if read operation fails
     */
    List<T> readByCriteria(Map<String, Object> criteria) throws CrudException;
    
    // UPDATE operations
    
    /**
     * Update an existing entity.
     * 
     * @param id The ID of the entity to update
     * @param entity The updated entity data
     * @return The updated entity
     * @throws CrudException if update fails or entity not found
     */
    T update(ID id, T entity) throws CrudException;
    
    /**
     * Update an entity (ID determined from entity).
     * 
     * @param entity The entity to update
     * @return The updated entity
     * @throws CrudException if update fails or entity not found
     */
    T update(T entity) throws CrudException;
    
    /**
     * Update multiple entities in batch.
     * 
     * @param entities List of entities to update
     * @return List of updated entities
     * @throws CrudException if batch update fails
     */
    List<T> updateAll(List<T> entities) throws CrudException;
    
    /**
     * Partial update of an entity.
     * 
     * @param id The ID of the entity to update
     * @param updates Map of field names to new values
     * @return The updated entity
     * @throws CrudException if update fails or entity not found
     */
    T partialUpdate(ID id, Map<String, Object> updates) throws CrudException;
    
    /**
     * Update or create entity (upsert operation).
     * 
     * @param id The ID of the entity
     * @param entity The entity data
     * @return The updated or created entity
     * @throws CrudException if operation fails
     */
    T upsert(ID id, T entity) throws CrudException;
    
    // DELETE operations
    
    /**
     * Delete an entity by ID.
     * 
     * @param id The ID of the entity to delete
     * @return true if entity was deleted, false if not found
     * @throws CrudException if delete operation fails
     */
    boolean delete(ID id) throws CrudException;
    
    /**
     * Delete an entity.
     * 
     * @param entity The entity to delete
     * @return true if entity was deleted, false if not found
     * @throws CrudException if delete operation fails
     */
    boolean delete(T entity) throws CrudException;
    
    /**
     * Delete multiple entities by IDs.
     * 
     * @param ids List of IDs to delete
     * @return Number of entities successfully deleted
     * @throws CrudException if batch delete operation fails
     */
    int deleteByIds(List<ID> ids) throws CrudException;
    
    /**
     * Delete multiple entities.
     * 
     * @param entities List of entities to delete
     * @return Number of entities successfully deleted
     * @throws CrudException if batch delete operation fails
     */
    int deleteAll(List<T> entities) throws CrudException;
    
    /**
     * Delete all entities.
     * 
     * @return Number of entities deleted
     * @throws CrudException if delete all operation fails
     */
    int deleteAll() throws CrudException;
    
    /**
     * Delete entities by field value.
     * 
     * @param fieldName The name of the field to match
     * @param value The value to match
     * @return Number of entities deleted
     * @throws CrudException if delete operation fails
     */
    int deleteByField(String fieldName, Object value) throws CrudException;
    
    /**
     * Delete entities by multiple field criteria.
     * 
     * @param criteria Map of field names to values
     * @return Number of entities deleted
     * @throws CrudException if delete operation fails
     */
    int deleteByCriteria(Map<String, Object> criteria) throws CrudException;
    
    // UTILITY operations
    
    /**
     * Check if an entity exists by ID.
     * 
     * @param id The ID to check
     * @return true if entity exists, false otherwise
     * @throws CrudException if existence check fails
     */
    boolean exists(ID id) throws CrudException;
    
    /**
     * Count total number of entities.
     * 
     * @return Total count of entities
     * @throws CrudException if count operation fails
     */
    long count() throws CrudException;
    
    /**
     * Count entities matching field criteria.
     * 
     * @param fieldName The name of the field to match
     * @param value The value to match
     * @return Count of matching entities
     * @throws CrudException if count operation fails
     */
    long countByField(String fieldName, Object value) throws CrudException;
    
    /**
     * Count entities matching multiple field criteria.
     * 
     * @param criteria Map of field names to values
     * @return Count of matching entities
     * @throws CrudException if count operation fails
     */
    long countByCriteria(Map<String, Object> criteria) throws CrudException;
    
    /**
     * Get all unique values for a specific field.
     * 
     * @param fieldName The name of the field
     * @return List of unique values
     * @throws CrudException if operation fails
     */
    List<Object> getUniqueValues(String fieldName) throws CrudException;
    
    // DEFAULT methods
    
    /**
     * Default method to check if repository is empty.
     * 
     * @return true if no entities exist, false otherwise
     * @throws CrudException if count operation fails
     */
    default boolean isEmpty() throws CrudException {
        return count() == 0;
    }
    
    /**
     * Default method to get first entity.
     * 
     * @return Optional containing the first entity, empty if none found
     * @throws CrudException if read operation fails
     */
    default Optional<T> getFirst() throws CrudException {
        List<T> entities = readWithPagination(0, 1);
        return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
    }
    
    /**
     * Default method to get last entity.
     * 
     * @return Optional containing the last entity, empty if none found
     * @throws CrudException if read operation fails
     */
    default Optional<T> getLast() throws CrudException {
        long totalCount = count();
        if (totalCount == 0) {
            return Optional.empty();
        }
        List<T> entities = readWithPagination((int) totalCount - 1, 1);
        return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
    }
    
    /**
     * Default method to create entity if it doesn't exist.
     * 
     * @param id The ID to check
     * @param entity The entity to create if not found
     * @return The existing or newly created entity
     * @throws CrudException if operation fails
     */
    default T createIfNotExists(ID id, T entity) throws CrudException {
        Optional<T> existing = read(id);
        if (existing.isPresent()) {
            return existing.get();
        }
        return createWithId(id, entity);
    }
    
    /**
     * Default method to read or create entity.
     * 
     * @param id The ID to check
     * @param defaultEntity The entity to create if not found
     * @return The existing or newly created entity
     * @throws CrudException if operation fails
     */
    default T readOrCreate(ID id, T defaultEntity) throws CrudException {
        Optional<T> existing = read(id);
        if (existing.isPresent()) {
            return existing.get();
        }
        return createWithId(id, defaultEntity);
    }
    
    /**
     * Default method for safe delete (returns success status).
     * 
     * @param id The ID of the entity to delete
     * @return true if deleted successfully, false if entity didn't exist
     * @throws CrudException if delete operation fails
     */
    default boolean safeDelete(ID id) throws CrudException {
        if (exists(id)) {
            return delete(id);
        }
        return false;
    }
    
    /**
     * Custom exception for CRUD operations.
     */
    class CrudException extends Exception {
        private final CrudOperation operation;
        private final Object entityId;
        private final String entityType;
        
        public enum CrudOperation {
            CREATE, READ, UPDATE, DELETE, COUNT, EXISTS
        }
        
        public CrudException(String message) {
            super(message);
            this.operation = null;
            this.entityId = null;
            this.entityType = null;
        }
        
        public CrudException(String message, Throwable cause) {
            super(message, cause);
            this.operation = null;
            this.entityId = null;
            this.entityType = null;
        }
        
        public CrudException(CrudOperation operation, Object entityId, String message) {
            super(String.format("CRUD operation '%s' failed for entity '%s': %s", operation, entityId, message));
            this.operation = operation;
            this.entityId = entityId;
            this.entityType = null;
        }
        
        public CrudException(CrudOperation operation, String entityType, String message) {
            super(String.format("CRUD operation '%s' failed for entity type '%s': %s", operation, entityType, message));
            this.operation = operation;
            this.entityId = null;
            this.entityType = entityType;
        }
        
        public CrudException(CrudOperation operation, Object entityId, String message, Throwable cause) {
            super(String.format("CRUD operation '%s' failed for entity '%s': %s", operation, entityId, message), cause);
            this.operation = operation;
            this.entityId = entityId;
            this.entityType = null;
        }
        
        public CrudOperation getOperation() { return operation; }
        public Object getEntityId() { return entityId; }
        public String getEntityType() { return entityType; }
    }
}