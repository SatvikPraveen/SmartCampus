package interfaces;

import interfaces.Searchable.SearchCriteria;
import interfaces.Searchable.SearchCriterion;
import interfaces.Searchable.SearchResult;
import interfaces.Searchable.SortOrder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class SearchableTest {

    /** Case-sensitive substring search over a fixed list of names. */
    private static final class NameSearch implements Searchable<String> {
        final List<String> names = List.of("alice smith", "bob smith", "alice jones", "Carol");
        final List<String> keywordsSearched = new ArrayList<>();

        @Override public List<String> search(String keyword) {
            keywordsSearched.add(keyword);
            return names.stream().filter(n -> n.contains(keyword)).toList();
        }
        @Override public List<String> filter(Predicate<String> predicate) { return names.stream().filter(predicate).toList(); }

        @Override public List<String> search(Map<String, SearchCriterion> c) { return List.of(); }
        @Override public List<String> search(Predicate<String> p) { return filter(p); }
        @Override public List<String> searchAndSort(String k, String s, SortOrder o) { return search(k); }
        @Override public SearchResult<String> searchWithPagination(String k, int p, int s) { return null; }
        @Override public SearchResult<String> advancedSearchWithPagination(Map<String, SearchCriterion> c, String s,
                                                                           SortOrder o, int p, int ps) { return null; }
        @Override public List<String> getSearchSuggestions(String partial, int max) { return List.of(); }
        @Override public List<String> getSearchableFields() { return List.of("name"); }
        @Override public List<String> getSortableFields() { return List.of("name"); }
        @Override public long countSearchResults(String keyword) { return search(keyword).size(); }
        @Override public long countSearchResults(Map<String, SearchCriterion> c) { return 0; }
    }

    @Nested
    class DefaultMethods {

        private final NameSearch searchable = new NameSearch();

        @Test
        void searchIgnoreCaseLowercasesTheKeyword() {
            assertThat(searchable.searchIgnoreCase("ALICE")).containsExactly("alice smith", "alice jones");
            assertThat(searchable.keywordsSearched).containsExactly("alice");
        }

        @Test
        void searchMultipleKeywordsRequiresEveryKeyword() {
            assertThat(searchable.searchMultipleKeywords(List.of("alice", "smith"))).containsExactly("alice smith");
            assertThat(searchable.searchMultipleKeywords(List.of("smith"))).containsExactly("alice smith", "bob smith");
            assertThat(searchable.searchMultipleKeywords(List.of("alice", "nobody"))).isEmpty();
        }

        @Test
        void searchMultipleKeywordsWithNoKeywordsReturnsNothing() {
            assertThat(searchable.searchMultipleKeywords(List.of())).isEmpty();
        }

        @Test
        void criterionFactories() {
            SearchCriterion exact = Searchable.createCriterion(SearchCriteria.EXACT_MATCH, "x");
            SearchCriterion range = Searchable.createRangeCriterion(1, 10);

            assertThat(exact.getCriteria()).isEqualTo(SearchCriteria.EXACT_MATCH);
            assertThat(exact.getValue()).isEqualTo("x");
            assertThat(exact.getSecondaryValue()).isNull();
            assertThat(exact.isCaseSensitive()).isFalse();
            assertThat(range.getCriteria()).isEqualTo(SearchCriteria.RANGE);
            assertThat(range.getValue()).isEqualTo(1);
            assertThat(range.getSecondaryValue()).isEqualTo(10);
        }
    }

    @Nested
    class Criteria {

        @Test
        void caseSensitiveConstructor() {
            SearchCriterion c = new SearchCriterion(SearchCriteria.CONTAINS, "Al", true);

            assertThat(c.isCaseSensitive()).isTrue();
            assertThat(c).hasToString("SearchCriterion{criteria=CONTAINS, value=Al, caseSensitive=true}");
        }

        @Test
        void rangeToStringShowsSecondaryValue() {
            assertThat(new SearchCriterion(SearchCriteria.BETWEEN, 1, 5))
                    .hasToString("SearchCriterion{criteria=BETWEEN, value=1, secondaryValue=5}");
        }

        @Test
        void enumDisplayNames() {
            assertThat(SearchCriteria.REGEX.getDisplayName()).isEqualTo("Regular Expression");
            assertThat(SortOrder.DESC.getDisplayName()).isEqualTo("Descending");
        }
    }

    @Nested
    class Results {

        @ParameterizedTest(name = "page {0}, size {1}, total {2}")
        @CsvSource({
                // page, pageSize, total, totalPages, hasNext, hasPrevious, firstPage, lastPage
                "0, 10, 25, 3, true, false, true, false",
                "1, 10, 25, 3, true, true, false, false",
                "2, 10, 25, 3, false, true, false, true",
                "0, 10, 5, 1, false, false, true, true"
        })
        void navigationFlags(int page, int pageSize, long total, int totalPages, boolean hasNext, boolean hasPrevious,
                             boolean firstPage, boolean lastPage) {
            SearchResult<String> r = new SearchResult<>(List.of(), page, pageSize, total);

            assertThat(r.getTotalPages()).isEqualTo(totalPages);
            assertThat(r.hasNext()).isEqualTo(hasNext);
            assertThat(r.hasPrevious()).isEqualTo(hasPrevious);
            assertThat(r.isFirstPage()).isEqualTo(firstPage);
            assertThat(r.isLastPage()).isEqualTo(lastPage);
        }

        // Regression: with zero results (totalPages == 0) page 0 was reported as not being the last page,
        // although hasNext() was false; Repository.Page already treated it as last.
        @Test
        void emptyResultIsBothFirstAndLastPage() {
            SearchResult<String> r = new SearchResult<>(List.of(), 0, 10, 0);

            assertThat(r.getTotalPages()).isZero();
            assertThat(r.hasNext()).isFalse();
            assertThat(r.isFirstPage()).isTrue();
            assertThat(r.isLastPage()).isTrue();
            assertThat(r.isEmpty()).isTrue();
        }

        @Test
        void accessorsAndToString() {
            SearchResult<String> r = new SearchResult<>(List.of("a", "b"), 1, 2, 6, "name", SortOrder.ASC);

            assertThat(r.getResults()).containsExactly("a", "b");
            assertThat(r.getPage()).isEqualTo(1);
            assertThat(r.getPageSize()).isEqualTo(2);
            assertThat(r.getTotalElements()).isEqualTo(6);
            assertThat(r.getResultCount()).isEqualTo(2);
            assertThat(r.getSortBy()).isEqualTo("name");
            assertThat(r.getSortOrder()).isEqualTo(SortOrder.ASC);
            assertThat(r).hasToString("SearchResult{page=2/3, size=2, total=6, hasNext=true, hasPrevious=true}");

            SearchResult<String> unsorted = new SearchResult<>(List.of(), 0, 2, 0);
            assertThat(unsorted.getSortBy()).isNull();
            assertThat(unsorted.getSortOrder()).isNull();
        }
    }
}
