// File location: src/main/java/repositories/BaseRepository.java

package repositories;

import interfaces.Repository;
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
public abstract class BaseRepository<T, ID> implements Repository<T, ID> {
    
    protected final Map<ID, T> storage = new ConcurrentHashMap<>();
    
    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        ID id = extractId(entity);
        if (id == null) {
            // never hand out an id that an explicitly-identified entity already occupies
            do {
                id = generateId();
            } while (storage.containsKey(id));
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
    public List<T> findAllById(List<ID> ids) {
        return ids.stream()
                .map(storage::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    public T update(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        ID id = extractId(entity);
        if (id == null || !storage.containsKey(id)) {
            throw new IllegalArgumentException("Entity does not exist: " + id);
        }
        
        storage.put(id, entity);
        return entity;
    }
    
    @Override
    public List<T> updateAll(List<T> entities) {
        return entities.stream()
                .map(this::update)
                .collect(Collectors.toList());
    }
    
    @Override
    public T refresh(T entity) {
        if (entity == null) {
            return null;
        }
        ID id = extractId(entity);
        return id != null ? storage.getOrDefault(id, entity) : entity;
    }
    
    @Override
    public void flush() {
        // In-memory storage: all changes are applied immediately
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
    public void deleteAllById(List<ID> ids) {
        ids.forEach(storage::remove);
    }
    
    @Override
    public void deleteAll(List<T> entities) {
        entities.forEach(this::delete);
    }
    
    @Override
    public void deleteAll() {
        storage.clear();
    }
    
    // Additional utility methods
    @Override
    public List<T> findByPredicate(Predicate<T> predicate) {
        return storage.values()
                .stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByPredicate(Predicate<T> predicate) {
        return storage.values()
                .stream()
                .filter(predicate)
                .count();
    }
    
    @Override
    public Optional<T> findFirstByPredicate(Predicate<T> predicate) {
        return storage.values()
                .stream()
                .filter(predicate)
                .findFirst();
    }
    
    @Override
    public List<T> saveAll(List<T> entities) {
        return entities.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }
    
    @Override
    public int deleteByPredicate(Predicate<T> predicate) {
        List<ID> idsToDelete = storage.entrySet()
                .stream()
                .filter(entry -> predicate.test(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        idsToDelete.forEach(storage::remove);
        return idsToDelete.size();
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
    
    @Override
    public Page<T> findAll(int page, int size) {
        return new Page<>(findWithPagination(page, size), page, size, count());
    }
    
    @Override
    public Page<T> findByPredicate(Predicate<T> predicate, int page, int size) {
        List<T> matching = findByPredicate(predicate);
        List<T> content = matching.stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
        return new Page<>(content, page, size, matching.size());
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