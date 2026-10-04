package interfaces;

import interfaces.Repository.Page;
import interfaces.Repository.RepositoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryTest {

    /** List-backed repository; only save/flush/count/findAll(page,size) matter for the default methods. */
    private static final class ListRepository implements Repository<String, Integer> {
        final List<String> items = new ArrayList<>();
        final List<String> events = new ArrayList<>();

        @Override public String save(String entity) { items.add(entity); events.add("save:" + entity); return entity; }
        @Override public void flush() { events.add("flush"); }
        @Override public long count() { return items.size(); }
        @Override public Page<String> findAll(int page, int size) {
            List<String> content = items.stream().skip((long) page * size).limit(size).toList();
            return new Page<>(content, page, size, items.size());
        }

        @Override public List<String> saveAll(List<String> entities) { entities.forEach(this::save); return entities; }
        @Override public Optional<String> findById(Integer id) { return Optional.empty(); }
        @Override public boolean existsById(Integer id) { return false; }
        @Override public List<String> findAll() { return items; }
        @Override public List<String> findAllById(List<Integer> ids) { return List.of(); }
        @Override public List<String> findByPredicate(Predicate<String> p) { return items.stream().filter(p).toList(); }
        @Override public Optional<String> findFirstByPredicate(Predicate<String> p) { return Optional.empty(); }
        @Override public long countByPredicate(Predicate<String> p) { return 0; }
        @Override public String update(String entity) { return entity; }
        @Override public List<String> updateAll(List<String> entities) { return entities; }
        @Override public void deleteById(Integer id) { }
        @Override public void delete(String entity) { }
        @Override public void deleteAllById(List<Integer> ids) { }
        @Override public void deleteAll(List<String> entities) { }
        @Override public void deleteAll() { items.clear(); }
        @Override public int deleteByPredicate(Predicate<String> p) { return 0; }
        @Override public Page<String> findByPredicate(Predicate<String> p, int page, int size) { return null; }
        @Override public String refresh(String entity) { return entity; }
    }

    @Nested
    class DefaultMethods {

        private ListRepository repo;

        @BeforeEach
        void setUp() {
            repo = new ListRepository();
        }

        @Test
        void saveAndFlushSavesThenFlushes() throws RepositoryException {
            assertThat(repo.saveAndFlush("a")).isEqualTo("a");
            assertThat(repo.events).containsExactly("save:a", "flush");
        }

        @Test
        void emptyRepository() throws RepositoryException {
            assertThat(repo.isEmpty()).isTrue();
            assertThat(repo.findFirst()).isEmpty();
            assertThat(repo.findLast()).isEmpty();
        }

        @Test
        void firstAndLast() throws RepositoryException {
            repo.saveAll(List.of("a", "b", "c"));

            assertThat(repo.isEmpty()).isFalse();
            assertThat(repo.findFirst()).contains("a");
            assertThat(repo.findLast()).contains("c");
        }
    }

    @Nested
    class Pages {

        @ParameterizedTest(name = "page {0} of {2} items, size {1}")
        @CsvSource({
                // page, size, total, totalPages, first, last, hasNext, hasPrevious
                "0, 10, 25, 3, true, false, true, false",
                "1, 10, 25, 3, false, false, true, true",
                "2, 10, 25, 3, false, true, false, true",
                "0, 10, 10, 1, true, true, false, false",
                "0, 10, 0, 0, true, true, false, false"
        })
        void navigationFlags(int page, int size, long total, int totalPages, boolean first, boolean last,
                             boolean hasNext, boolean hasPrevious) {
            Page<String> p = new Page<>(List.of(), page, size, total);

            assertThat(p.getTotalPages()).isEqualTo(totalPages);
            assertThat(p.isFirst()).isEqualTo(first);
            assertThat(p.isLast()).isEqualTo(last);
            assertThat(p.hasNext()).isEqualTo(hasNext);
            assertThat(p.hasPrevious()).isEqualTo(hasPrevious);
        }

        @Test
        void contentAccessors() {
            Page<String> p = new Page<>(List.of("a", "b"), 1, 2, 5);

            assertThat(p.getContent()).containsExactly("a", "b");
            assertThat(p.getPage()).isEqualTo(1);
            assertThat(p.getSize()).isEqualTo(2);
            assertThat(p.getTotalElements()).isEqualTo(5);
            assertThat(p.getNumberOfElements()).isEqualTo(2);
            assertThat(p.isEmpty()).isFalse();
            assertThat(new Page<String>(List.of(), 0, 2, 0).isEmpty()).isTrue();
            assertThat(p).hasToString("Page{page=1, size=2, totalElements=5, totalPages=3, content=2 items}");
        }
    }

    @Nested
    class Exceptions {

        @Test
        void plainConstructors() {
            Throwable cause = new IllegalStateException();
            RepositoryException plain = new RepositoryException("m");

            assertThat(plain).hasMessage("m");
            assertThat(plain.getOperation()).isNull();
            assertThat(plain.getEntityId()).isNull();
            assertThat(new RepositoryException("m", cause)).hasCause(cause);
        }

        @Test
        void detailedConstructors() {
            RepositoryException e = new RepositoryException("save", 7, "constraint");
            assertThat(e).hasMessage("Repository operation 'save' failed for entity '7': constraint");
            assertThat(e.getOperation()).isEqualTo("save");
            assertThat(e.getEntityId()).isEqualTo(7);

            Throwable cause = new RuntimeException();
            RepositoryException withCause = new RepositoryException("delete", "S1", "locked", cause);
            assertThat(withCause).hasCause(cause).hasMessage("Repository operation 'delete' failed for entity 'S1': locked");
            assertThat(withCause.getEntityId()).isEqualTo("S1");
        }
    }
}
