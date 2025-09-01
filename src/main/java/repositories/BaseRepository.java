// File location: src/main/java/repositories/BaseRepository.java

package repositories;

import interfaces.Repository;
import interfaces.CrudOperations;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic base repository implementation providing common CRUD operations
 * Uses concurrent collections for thread safety
 * @param <T> The entity type
 * @param <ID> The ID type
 */
public abstract class BaseRepository<T, ID> implements Repository<T, ID>, CrudOperations<T, ID> {
    
    protected final Map<ID, T> storage = new ConcurrentHashMap<>();
    
    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        ID id = extractId(entity);
        if (id == null) {
            id = generateId();
            setId(entity, id);
        }
        
        storage.put(id, entity);
        return entity;
    }
    
    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(storage.get(id));
    }
    
    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }
    
    @Override
    public void deleteById(ID id) {
        storage.remove(id);
    }
    
    @Override
    public void delete(T entity) {
        if (entity != null) {
            ID id = extractId(entity);
            if (id != null) {
                storage.remove(id);
            }
        }
    }
    
    @Override
    public boolean existsById(ID id) {
        return storage.containsKey(id);
    }
    
    @Override
    public long count() {
        return storage.size();
    }
    
    @Override
    public void deleteAll() {
        storage.clear();
    }
    
    // Additional utility methods
    public List<T> findByPredicate(Predicate<T> predicate) {
        return storage.values()
                .stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    public Optional<T> findFirstByPredicate(Predicate<T> predicate) {
        return storage.values()
                .stream()
                .filter(predicate)
                .findFirst();
    }
    
    public List<T> saveAll(List<T> entities) {
        return entities.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }
    
    public void deleteByPredicate(Predicate<T> predicate) {
        List<ID> idsToDelete = storage.entrySet()
                .stream()
                .filter(entry -> predicate.test(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        idsToDelete.forEach(storage::remove);
    }
    
    // Abstract methods to be implemented by concrete repositories
    protected abstract ID extractId(T entity);
    protected abstract void setId(T entity, ID id);
    protected abstract ID generateId();
    
    // Pagination support
    public List<T> findWithPagination(int page, int size) {
        return storage.values()
                .stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }
    
    // Sorting support
    public List<T> findAllSorted(Comparator<T> comparator) {
        return storage.values()
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    // Batch operations
    public Map<ID, T> findByIds(Collection<ID> ids) {
        return ids.stream()
                .filter(storage::containsKey)
                .collect(Collectors.toMap(
                    id -> id,
                    storage::get
                ));
    }
    
    public void deleteByIds(Collection<ID> ids) {
        ids.forEach(storage::remove);
    }
    
    // Statistics
    public Map<String, Object> getRepositoryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntities", count());
        stats.put("repositoryClass", this.getClass().getSimpleName());
        stats.put("lastModified", new Date());
        return stats;
    }
}