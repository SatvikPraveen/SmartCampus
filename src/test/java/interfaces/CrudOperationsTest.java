package interfaces;

import interfaces.CrudOperations.CrudException;
import interfaces.CrudOperations.CrudException.CrudOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrudOperationsTest {

    /** Minimal ordered in-memory store; only the operations the default methods rely on do real work. */
    private static final class InMemoryCrud implements CrudOperations<String, Integer> {
        final TreeMap<Integer, String> store = new TreeMap<>();
        int createWithIdCalls;
        int deleteCalls;

        @Override public String createWithId(Integer id, String entity) throws CrudException {
            createWithIdCalls++;
            if (store.containsKey(id)) {
                throw new CrudException(CrudOperation.CREATE, (Object) id, "duplicate");
            }
            store.put(id, entity);
            return entity;
        }
        @Override public Optional<String> read(Integer id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<String> readWithPagination(int offset, int limit) {
            return store.values().stream().skip(offset).limit(limit).toList();
        }
        @Override public boolean delete(Integer id) { deleteCalls++; return store.remove(id) != null; }
        @Override public boolean exists(Integer id) { return store.containsKey(id); }
        @Override public long count() { return store.size(); }

        @Override public String create(String entity) { throw new UnsupportedOperationException(); }
        @Override public List<String> createAll(List<String> entities) { throw new UnsupportedOperationException(); }
        @Override public List<String> readAll() { return new ArrayList<>(store.values()); }
        @Override public List<String> readByIds(List<Integer> ids) { return List.of(); }
        @Override public List<String> readByField(String f, Object v) { return List.of(); }
        @Override public List<String> readByCriteria(Map<String, Object> c) { return List.of(); }
        @Override public String update(Integer id, String entity) { return entity; }
        @Override public String update(String entity) { return entity; }
        @Override public List<String> updateAll(List<String> entities) { return entities; }
        @Override public String partialUpdate(Integer id, Map<String, Object> u) { return null; }
        @Override public String upsert(Integer id, String entity) { return entity; }
        @Override public boolean deleteEntity(String entity) { return false; }
        @Override public int deleteByIds(List<Integer> ids) { return 0; }
        @Override public int deleteAll(List<String> entities) { return 0; }
        @Override public int deleteAll() { return 0; }
        @Override public int deleteByField(String f, Object v) { return 0; }
        @Override public int deleteByCriteria(Map<String, Object> c) { return 0; }
        @Override public long countByField(String f, Object v) { return 0; }
        @Override public long countByCriteria(Map<String, Object> c) { return 0; }
        @Override public List<Object> getUniqueValues(String f) { return List.of(); }
    }

    @Nested
    class DefaultMethods {

        private InMemoryCrud crud;

        @BeforeEach
        void setUp() {
            crud = new InMemoryCrud();
        }

        @Test
        void emptyStore() throws CrudException {
            assertThat(crud.isEmpty()).isTrue();
            assertThat(crud.getFirst()).isEmpty();
            assertThat(crud.getLast()).isEmpty();
        }

        @Test
        void firstAndLastFollowStoreOrder() throws CrudException {
            crud.store.put(2, "b");
            crud.store.put(1, "a");
            crud.store.put(3, "c");

            assertThat(crud.isEmpty()).isFalse();
            assertThat(crud.getFirst()).contains("a");
            assertThat(crud.getLast()).contains("c");
        }

        @Test
        void createIfNotExistsReturnsExistingWithoutCreating() throws CrudException {
            crud.store.put(1, "existing");

            assertThat(crud.createIfNotExists(1, "new")).isEqualTo("existing");
            assertThat(crud.createWithIdCalls).isZero();
            assertThat(crud.createIfNotExists(2, "new")).isEqualTo("new");
            assertThat(crud.store).containsEntry(2, "new");
        }

        @Test
        void readOrCreateBehavesTheSame() throws CrudException {
            crud.store.put(1, "existing");

            assertThat(crud.readOrCreate(1, "default")).isEqualTo("existing");
            assertThat(crud.readOrCreate(5, "default")).isEqualTo("default");
            assertThat(crud.createWithIdCalls).isEqualTo(1);
        }

        @Test
        void safeDeleteOnlyDeletesExisting() throws CrudException {
            crud.store.put(1, "a");

            assertThat(crud.safeDelete(9)).isFalse();
            assertThat(crud.deleteCalls).isZero();
            assertThat(crud.safeDelete(1)).isTrue();
            assertThat(crud.store).isEmpty();
        }

        @Test
        void exceptionsFromImplementationPropagate() {
            crud.store.put(1, "a");

            assertThatThrownBy(() -> crud.createWithId(1, "dup"))
                    .isInstanceOf(CrudException.class)
                    .hasMessage("CRUD operation 'CREATE' failed for entity '1': duplicate");
        }
    }

    @Nested
    class Exceptions {

        @Test
        void plainConstructorsHaveNoContext() {
            Throwable cause = new IllegalStateException();
            CrudException plain = new CrudException("m");
            CrudException withCause = new CrudException("m", cause);

            assertThat(plain).hasMessage("m");
            assertThat(plain.getOperation()).isNull();
            assertThat(plain.getEntityId()).isNull();
            assertThat(plain.getEntityType()).isNull();
            assertThat(withCause).hasCause(cause);
            assertThat(withCause.getOperation()).isNull();
        }

        @Test
        void entityIdConstructors() {
            CrudException e = new CrudException(CrudOperation.READ, (Object) 42L, "missing");
            assertThat(e).hasMessage("CRUD operation 'READ' failed for entity '42': missing");
            assertThat(e.getOperation()).isEqualTo(CrudOperation.READ);
            assertThat(e.getEntityId()).isEqualTo(42L);
            assertThat(e.getEntityType()).isNull();

            Throwable cause = new RuntimeException();
            CrudException withCause = new CrudException(CrudOperation.DELETE, 7, "locked", cause);
            assertThat(withCause).hasCause(cause)
                    .hasMessage("CRUD operation 'DELETE' failed for entity '7': locked");
            assertThat(withCause.getEntityId()).isEqualTo(7);
        }

        @Test
        void entityTypeConstructor() {
            CrudException e = new CrudException(CrudOperation.COUNT, "Student", "db down");

            assertThat(e).hasMessage("CRUD operation 'COUNT' failed for entity type 'Student': db down");
            assertThat(e.getEntityType()).isEqualTo("Student");
            assertThat(e.getEntityId()).isNull();
        }

        @Test
        void operationsEnum() {
            assertThat(CrudOperation.values()).containsExactly(CrudOperation.CREATE, CrudOperation.READ,
                    CrudOperation.UPDATE, CrudOperation.DELETE, CrudOperation.COUNT, CrudOperation.EXISTS);
        }
    }
}
