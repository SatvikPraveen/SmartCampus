package repositories;

import interfaces.Repository.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseRepositoryTest {

    static final class Item {
        String id;
        final String name;
        final int value;

        Item(String id, String name, int value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return id + ":" + name;
        }
    }

    static final class ItemRepository extends BaseRepository<Item, String> {
        private final AtomicLong seq = new AtomicLong(1);

        @Override protected String extractId(Item item) { return item.id; }
        @Override protected void setId(Item item, String id) { item.id = id; }
        @Override protected String generateId() { return "I" + seq.getAndIncrement(); }
    }

    private ItemRepository repo;

    @BeforeEach
    void setUp() {
        repo = new ItemRepository();
    }

    private List<Item> seed(int n) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < n; i++) items.add(repo.save(new Item(null, "item" + i, i)));
        return items;
    }

    @Nested
    class Crud {

        @Test
        void saveAssignsGeneratedIdWhenMissing() throws Exception {
            Item item = repo.save(new Item(null, "a", 1));

            assertThat(item.id).isEqualTo("I1");
            assertThat(repo.findById("I1")).containsSame(item);
            assertThat(repo.save(new Item(null, "b", 2)).id).isEqualTo("I2");
        }

        @Test
        void saveKeepsExplicitIdAndReplacesExistingEntity() throws Exception {
            Item first = repo.save(new Item("X", "a", 1));
            Item replacement = repo.save(new Item("X", "b", 2));

            assertThat(repo.count()).isEqualTo(1);
            assertThat(repo.findById("X")).containsSame(replacement);
            assertThat(first).isNotSameAs(replacement);
        }

        @Test
        void generatedIdNeverOverwritesExplicitlySavedEntity() throws Exception {
            // Regression: an entity saved with an id the generator later produced was silently replaced.
            Item manual = repo.save(new Item("I1", "manual", 0));

            Item generated = repo.save(new Item(null, "generated", 1));

            assertThat(generated.id).isNotEqualTo("I1");
            assertThat(repo.findById("I1")).containsSame(manual);
            assertThat(repo.count()).isEqualTo(2);
        }

        @Test
        void saveRejectsNull() throws Exception {
            assertThatThrownBy(() -> repo.save(null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void updateRequiresExistingEntity() throws Exception {
            Item item = repo.save(new Item("A", "a", 1));
            Item changed = new Item("A", "a2", 2);

            assertThat(repo.update(changed)).isSameAs(changed);
            assertThat(repo.findById("A")).containsSame(changed);
            assertThatThrownBy(() -> repo.update(new Item("missing", "x", 0))).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repo.update(new Item(null, "x", 0))).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repo.update(null)).isInstanceOf(IllegalArgumentException.class);
            assertThat(item).isNotNull();
        }

        @Test
        void updateAllFailsOnFirstMissingEntity() throws Exception {
            repo.save(new Item("A", "a", 1));

            assertThatThrownBy(() -> repo.updateAll(List.of(new Item("A", "a2", 2), new Item("B", "b", 3))))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(repo.existsById("B")).isFalse();
        }

        @Test
        void deleteVariants() throws Exception {
            List<Item> items = seed(6);

            repo.deleteById(items.get(0).id);
            repo.delete(items.get(1));
            repo.delete(null);
            repo.delete(new Item(null, "unsaved", 0));
            repo.deleteAllById(List.of(items.get(2).id, "unknown"));
            repo.deleteAll(List.of(items.get(3)));
            repo.deleteByIds(Set.of(items.get(4).id));

            assertThat(repo.findAll()).containsExactly(items.get(5));
            repo.deleteAll();
            assertThat(repo.isEmpty()).isTrue();
        }

        @Test
        void deleteByPredicateReturnsNumberRemoved() throws Exception {
            seed(10);

            assertThat(repo.deleteByPredicate(i -> i.value % 2 == 0)).isEqualTo(5);
            assertThat(repo.deleteByPredicate(i -> i.value % 2 == 0)).isZero();
            assertThat(repo.findAll()).allMatch(i -> i.value % 2 == 1);
        }

        @Test
        void refreshReturnsStoredInstance() throws Exception {
            Item stored = repo.save(new Item("A", "stored", 1));

            assertThat(repo.refresh(new Item("A", "stale", 0))).isSameAs(stored);
            Item unsaved = new Item("B", "x", 0);
            assertThat(repo.refresh(unsaved)).isSameAs(unsaved);
            assertThat(repo.refresh(null)).isNull();
        }

        @Test
        void saveAndFlush() throws Exception {
            Item item = repo.saveAndFlush(new Item(null, "a", 1));
            assertThat(repo.existsById(item.id)).isTrue();
        }
    }

    @Nested
    class Queries {

        @Test
        void findAllByIdSkipsUnknownIdsAndPreservesRequestOrder() throws Exception {
            List<Item> items = seed(3);

            assertThat(repo.findAllById(List.of(items.get(2).id, "nope", items.get(0).id)))
                    .containsExactly(items.get(2), items.get(0));
            assertThat(repo.findByIds(List.of(items.get(1).id, "nope"))).containsOnlyKeys(items.get(1).id);
        }

        @Test
        void predicateQueries() throws Exception {
            seed(10);

            assertThat(repo.findByPredicate(i -> i.value >= 7)).extracting(i -> i.value)
                    .containsExactlyInAnyOrder(7, 8, 9);
            assertThat(repo.countByPredicate(i -> i.value < 3)).isEqualTo(3);
            assertThat(repo.findFirstByPredicate(i -> i.value == 4)).get().extracting(i -> i.name).isEqualTo("item4");
            assertThat(repo.findFirstByPredicate(i -> i.value > 100)).isEmpty();
        }

        @Test
        void findAllReturnsDefensiveCopy() throws Exception {
            seed(2);
            List<Item> all = repo.findAll();
            all.clear();

            assertThat(repo.count()).isEqualTo(2);
        }

        @Test
        void findAllSorted() throws Exception {
            seed(5);

            assertThat(repo.findAllSorted(Comparator.comparingInt((Item i) -> i.value).reversed()))
                    .extracting(i -> i.value).containsExactly(4, 3, 2, 1, 0);
        }

        @Test
        void statsReportCountAndClass() throws Exception {
            seed(3);

            assertThat(repo.getRepositoryStats())
                    .containsEntry("totalEntities", 3L)
                    .containsEntry("repositoryClass", "ItemRepository")
                    .containsKey("lastModified");
        }
    }

    @Nested
    class Paging {

        @ParameterizedTest(name = "{0} items, page {1} size {2}")
        @CsvSource({
                "10, 0, 3, 3, 4, true,  false",
                "10, 3, 3, 1, 4, false, true",
                "10, 4, 3, 0, 4, false, true",
                "9,  2, 3, 3, 3, false, true",
                "0,  0, 5, 0, 0, true,  true",
        })
        void pageMetadata(int total, int page, int size, int expectedElements, int totalPages,
                          boolean first, boolean last) {
            seed(total);

            Page<Item> result = repo.findAll(page, size);

            assertThat(result.getNumberOfElements()).isEqualTo(expectedElements);
            assertThat(result.getTotalElements()).isEqualTo(total);
            assertThat(result.getTotalPages()).isEqualTo(totalPages);
            assertThat(result.isFirst()).isEqualTo(first);
            assertThat(result.isLast()).isEqualTo(last);
        }

        @Test
        void pagesPartitionAllEntities() throws Exception {
            seed(11);
            Set<String> seen = new HashSet<>();
            int pages = repo.findAll(0, 4).getTotalPages();

            for (int p = 0; p < pages; p++) {
                for (Item item : repo.findAll(p, 4).getContent()) {
                    assertThat(seen.add(item.id)).as("duplicate across pages: %s", item).isTrue();
                }
            }

            assertThat(seen).hasSize(11);
        }

        @Test
        void predicatePagingCountsOnlyMatches() throws Exception {
            seed(10);

            Page<Item> page = repo.findByPredicate(i -> i.value % 2 == 0, 1, 2);

            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
            assertThat(page.getContent()).hasSize(2).allMatch(i -> i.value % 2 == 0);
            assertThat(page.hasNext()).isTrue();
            assertThat(page.hasPrevious()).isTrue();
        }

        @Test
        void firstAndLast() throws Exception {
            assertThat(repo.findFirst()).isEmpty();
            assertThat(repo.findLast()).isEmpty();

            List<Item> items = seed(3);

            assertThat(repo.findFirst()).get().isIn(items);
            assertThat(repo.findLast()).get().isIn(items);
        }
    }
}
