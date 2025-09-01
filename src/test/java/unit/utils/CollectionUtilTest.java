// File location: src/test/java/unit/utils/CollectionUtilTest.java

package com.smartcampus.test.unit.utils;

import com.smartcampus.utils.CollectionUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Unit tests for the CollectionUtil class
 * Tests various collection utility methods and operations
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@DisplayName("Collection Util Tests")
class CollectionUtilTest {

    @Nested
    @DisplayName("Null and Empty Collection Tests")
    class NullAndEmptyCollectionTests {

        @Test
        @DisplayName("Should check if collection is null or empty")
        void shouldCheckIfCollectionIsNullOrEmpty() {
            assertTrue(CollectionUtil.isNullOrEmpty((List<String>) null));
            assertTrue(CollectionUtil.isNullOrEmpty(Collections.emptyList()));
            assertTrue(CollectionUtil.isNullOrEmpty(new ArrayList<>()));
            assertFalse(CollectionUtil.isNullOrEmpty(Arrays.asList("item")));
        }

        @Test
        @DisplayName("Should check if collection is not null or empty")
        void shouldCheckIfCollectionIsNotNullOrEmpty() {
            assertFalse(CollectionUtil.isNotNullOrEmpty((List<String>) null));
            assertFalse(CollectionUtil.isNotNullOrEmpty(Collections.emptyList()));
            assertTrue(CollectionUtil.isNotNullOrEmpty(Arrays.asList("item")));
            assertTrue(CollectionUtil.isNotNullOrEmpty(Arrays.asList("item1", "item2")));
        }

        @Test
        @DisplayName("Should get size safely")
        void shouldGetSizeSafely() {
            assertThat(CollectionUtil.safeSize(null)).isEqualTo(0);
            assertThat(CollectionUtil.safeSize(Collections.emptyList())).isEqualTo(0);
            assertThat(CollectionUtil.safeSize(Arrays.asList("a", "b", "c"))).isEqualTo(3);
        }

        @Test
        @DisplayName("Should provide default if collection is null or empty")
        void shouldProvideDefaultIfCollectionIsNullOrEmpty() {
            List<String> defaultList = Arrays.asList("default");
            
            assertThat(CollectionUtil.defaultIfEmpty(null, defaultList)).isEqualTo(defaultList);
            assertThat(CollectionUtil.defaultIfEmpty(Collections.emptyList(), defaultList)).isEqualTo(defaultList);
            
            List<String> nonEmptyList = Arrays.asList("item");
            assertThat(CollectionUtil.defaultIfEmpty(nonEmptyList, defaultList)).isEqualTo(nonEmptyList);
        }

        @Test
        @DisplayName("Should get first element safely")
        void shouldGetFirstElementSafely() {
            assertThat(CollectionUtil.getFirst(null)).isNull();
            assertThat(CollectionUtil.getFirst(Collections.emptyList())).isNull();
            assertThat(CollectionUtil.getFirst(Arrays.asList("first", "second"))).isEqualTo("first");
        }

        @Test
        @DisplayName("Should get last element safely")
        void shouldGetLastElementSafely() {
            assertThat(CollectionUtil.getLast(null)).isNull();
            assertThat(CollectionUtil.getLast(Collections.emptyList())).isNull();
            assertThat(CollectionUtil.getLast(Arrays.asList("first", "last"))).isEqualTo("last");
            assertThat(CollectionUtil.getLast(Arrays.asList("only"))).isEqualTo("only");
        }
    }

    @Nested
    @DisplayName("Collection Filtering Tests")
    class CollectionFilteringTests {

        @Test
        @DisplayName("Should filter collection by predicate")
        void shouldFilterCollectionByPredicate() {
            List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            Predicate<Integer> evenNumbers = n -> n % 2 == 0;
            
            List<Integer> filtered = CollectionUtil.filter(numbers, evenNumbers);
            
            assertThat(filtered).containsExactly(2, 4, 6, 8, 10);
        }

        @Test
        @DisplayName("Should handle null collection in filter")
        void shouldHandleNullCollectionInFilter() {
            List<Integer> filtered = CollectionUtil.filter(null, n -> n > 0);
            
            assertThat(filtered).isEmpty();
        }

        @Test
        @DisplayName("Should remove nulls from collection")
        void shouldRemoveNullsFromCollection() {
            List<String> listWithNulls = Arrays.asList("a", null, "b", null, "c");
            
            List<String> cleaned = CollectionUtil.removeNulls(listWithNulls);
            
            assertThat(cleaned).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("Should remove duplicates from collection")
        void shouldRemoveDuplicatesFromCollection() {
            List<String> listWithDuplicates = Arrays.asList("a", "b", "a", "c", "b", "d");
            
            List<String> unique = CollectionUtil.removeDuplicates(listWithDuplicates);
            
            assertThat(unique).containsExactly("a", "b", "c", "d");
        }

        @Test
        @DisplayName("Should filter by type")
        void shouldFilterByType() {
            List<Object> mixedList = Arrays.asList("string1", 123, "string2", 456, true);
            
            List<String> strings = CollectionUtil.filterByType(mixedList, String.class);
            List<Integer> integers = CollectionUtil.filterByType(mixedList, Integer.class);
            
            assertThat(strings).containsExactly("string1", "string2");
            assertThat(integers).containsExactly(123, 456);
        }

        @Test
        @DisplayName("Should find elements matching criteria")
        void shouldFindElementsMatchingCriteria() {
            List<String> words = Arrays.asList("apple", "banana", "apricot", "orange", "avocado");
            
            List<String> wordsStartingWithA = CollectionUtil.findAll(words, s -> s.startsWith("a"));
            
            assertThat(wordsStartingWithA).containsExactly("apple", "apricot", "avocado");
        }
    }

    @Nested
    @DisplayName("Collection Transformation Tests")
    class CollectionTransformationTests {

        @Test
        @DisplayName("Should map collection elements")
        void shouldMapCollectionElements() {
            List<String> words = Arrays.asList("hello", "world", "java");
            Function<String, Integer> lengthMapper = String::length;
            
            List<Integer> lengths = CollectionUtil.map(words, lengthMapper);
            
            assertThat(lengths).containsExactly(5, 5, 4);
        }

        @Test
        @DisplayName("Should handle null collection in map")
        void shouldHandleNullCollectionInMap() {
            List<Integer> mapped = CollectionUtil.map(null, String::length);
            
            assertThat(mapped).isEmpty();
        }

        @Test
        @DisplayName("Should flat map nested collections")
        void shouldFlatMapNestedCollections() {
            List<List<String>> nestedList = Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d", "e"),
                Arrays.asList("f")
            );
            
            List<String> flattened = CollectionUtil.flatMap(nestedList);
            
            assertThat(flattened).containsExactly("a", "b", "c", "d", "e", "f");
        }

        @Test
        @DisplayName("Should convert to different collection types")
        void shouldConvertToDifferentCollectionTypes() {
            List<String> list = Arrays.asList("a", "b", "c");
            
            Set<String> set = CollectionUtil.toSet(list);
            LinkedHashSet<String> linkedSet = CollectionUtil.toLinkedSet(list);
            TreeSet<String> treeSet = CollectionUtil.toTreeSet(list);
            
            assertThat(set).containsExactlyInAnyOrder("a", "b", "c");
            assertThat(linkedSet).containsExactly("a", "b", "c"); // Maintains order
            assertThat(treeSet).containsExactly("a", "b", "c"); // Sorted order
        }

        @Test
        @DisplayName("Should reverse collection")
        void shouldReverseCollection() {
            List<String> original = Arrays.asList("first", "second", "third");
            
            List<String> reversed = CollectionUtil.reverse(original);
            
            assertThat(reversed).containsExactly("third", "second", "first");
            assertThat(original).containsExactly("first", "second", "third"); // Original unchanged
        }

        @Test
        @DisplayName("Should shuffle collection")
        void shouldShuffleCollection() {
            List<Integer> original = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            
            List<Integer> shuffled = CollectionUtil.shuffle(original);
            
            assertThat(shuffled).hasSize(10);
            assertThat(shuffled).containsExactlyInAnyOrderElementsOf(original);
            // Note: There's a very small chance this could fail if shuffle returns original order
        }
    }

    @Nested
    @DisplayName("Collection Grouping and Partitioning Tests")
    class CollectionGroupingAndPartitioningTests {

        @Test
        @DisplayName("Should group elements by classifier")
        void shouldGroupElementsByClassifier() {
            List<String> words = Arrays.asList("apple", "banana", "apricot", "berry", "avocado");
            
            Map<Character, List<String>> groupedByFirstLetter = 
                CollectionUtil.groupBy(words, s -> s.charAt(0));
            
            assertThat(groupedByFirstLetter.get('a')).containsExactly("apple", "apricot", "avocado");
            assertThat(groupedByFirstLetter.get('b')).containsExactly("banana", "berry");
        }

        @Test
        @DisplayName("Should partition collection by predicate")
        void shouldPartitionCollectionByPredicate() {
            List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            
            Map<Boolean, List<Integer>> partitioned = 
                CollectionUtil.partition(numbers, n -> n % 2 == 0);
            
            assertThat(partitioned.get(true)).containsExactly(2, 4, 6, 8, 10); // Even numbers
            assertThat(partitioned.get(false)).containsExactly(1, 3, 5, 7, 9); // Odd numbers
        }

        @Test
        @DisplayName("Should chunk collection into smaller lists")
        void shouldChunkCollectionIntoSmallerLists() {
            List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            
            List<List<Integer>> chunks = CollectionUtil.chunk(numbers, 3);
            
            assertThat(chunks).hasSize(4);
            assertThat(chunks.get(0)).containsExactly(1, 2, 3);
            assertThat(chunks.get(1)).containsExactly(4, 5, 6);
            assertThat(chunks.get(2)).containsExactly(7, 8, 9);
            assertThat(chunks.get(3)).containsExactly(10);
        }

        @Test
        @DisplayName("Should batch process collection")
        void shouldBatchProcessCollection() {
            List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f", "g");
            List<List<String>> processedBatches = new ArrayList<>();
            
            CollectionUtil.processBatches(items, 3, batch -> processedBatches.add(new ArrayList<>(batch)));
            
            assertThat(processedBatches).hasSize(3);
            assertThat(processedBatches.get(0)).containsExactly("a", "b", "c");
            assertThat(processedBatches.get(1)).containsExactly("d", "e", "f");
            assertThat(processedBatches.get(2)).containsExactly("g");
        }
    }

    @Nested
    @DisplayName("Collection Search and Find Tests")
    class CollectionSearchAndFindTests {

        @Test
        @DisplayName("Should find first element matching predicate")
        void shouldFindFirstElementMatchingPredicate() {
            List<String> words = Arrays.asList("apple", "banana", "apricot", "orange");
            
            Optional<String> firstWordStartingWithA = 
                CollectionUtil.findFirst(words, s -> s.startsWith("a"));
            Optional<String> firstWordStartingWithZ = 
                CollectionUtil.findFirst(words, s -> s.startsWith("z"));
            
            assertThat(firstWordStartingWithA).hasValue("apple");
            assertThat(firstWordStartingWithZ).isEmpty();
        }

        @Test
        @DisplayName("Should check if any element matches predicate")
        void shouldCheckIfAnyElementMatchesPredicate() {
            List<Integer> numbers = Arrays.asList(1, 3, 5, 7, 8, 9);
            
            boolean hasEvenNumber = CollectionUtil.anyMatch(numbers, n -> n % 2 == 0);
            boolean hasNegativeNumber = CollectionUtil.anyMatch(numbers, n -> n < 0);
            
            assertTrue(hasEvenNumber);
            assertFalse(hasNegativeNumber);
        }

        @Test
        @DisplayName("Should check if all elements match predicate")
        void shouldCheckIfAllElementsMatchPredicate() {
            List<Integer> positiveNumbers = Arrays.asList(1, 2, 3, 4, 5);
            List<Integer> mixedNumbers = Arrays.asList(1, 2, -3, 4, 5);
            
            boolean allPositive1 = CollectionUtil.allMatch(positiveNumbers, n -> n > 0);
            boolean allPositive2 = CollectionUtil.allMatch(mixedNumbers, n -> n > 0);
            
            assertTrue(allPositive1);
            assertFalse(allPositive2);
        }

        @Test
        @DisplayName("Should check if no elements match predicate")
        void shouldCheckIfNoElementsMatchPredicate() {
            List<Integer> positiveNumbers = Arrays.asList(1, 2, 3, 4, 5);
            
            boolean noNegativeNumbers = CollectionUtil.noneMatch(positiveNumbers, n -> n < 0);
            boolean noEvenNumbers = CollectionUtil.noneMatch(positiveNumbers, n -> n % 2 == 0);
            
            assertTrue(noNegativeNumbers);
            assertFalse(noEvenNumbers);
        }

        @Test
        @DisplayName("Should find index of element")
        void shouldFindIndexOfElement() {
            List<String> words = Arrays.asList("apple", "banana", "apricot", "orange");
            
            int indexOfBanana = CollectionUtil.indexOf(words, "banana");
            int indexOfGrape = CollectionUtil.indexOf(words, "grape");
            
            assertThat(indexOfBanana).isEqualTo(1);
            assertThat(indexOfGrape).isEqualTo(-1);
        }

        @Test
        @DisplayName("Should find index of element matching predicate")
        void shouldFindIndexOfElementMatchingPredicate() {
            List<String> words = Arrays.asList("apple", "banana", "apricot", "orange");
            
            int indexOfFirstLongWord = CollectionUtil.indexOfFirst(words, s -> s.length() > 5);
            int indexOfWordStartingWithZ = CollectionUtil.indexOfFirst(words, s -> s.startsWith("z"));
            
            assertThat(indexOfFirstLongWord).isEqualTo(1); // "banana"
            assertThat(indexOfWordStartingWithZ).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Collection Comparison Tests")
    class CollectionComparisonTests {

        @Test
        @DisplayName("Should check if collections are equal")
        void shouldCheckIfCollectionsAreEqual() {
            List<String> list1 = Arrays.asList("a", "b", "c");
            List<String> list2 = Arrays.asList("a", "b", "c");
            List<String> list3 = Arrays.asList("a", "c", "b");
            List<String> list4 = Arrays.asList("a", "b");
            
            assertTrue(CollectionUtil.areEqual(list1, list2));
            assertFalse(CollectionUtil.areEqual(list1, list3)); // Different order
            assertFalse(CollectionUtil.areEqual(list1, list4)); // Different size
            assertFalse(CollectionUtil.areEqual(list1, null));
            assertTrue(CollectionUtil.areEqual(null, null));
        }

        @Test
        @DisplayName("Should check if collections are equal ignoring order")
        void shouldCheckIfCollectionsAreEqualIgnoringOrder() {
            List<String> list1 = Arrays.asList("a", "b", "c");
            List<String> list2 = Arrays.asList("c", "a", "b");
            List<String> list3 = Arrays.asList("a", "b", "c", "d");
            
            assertTrue(CollectionUtil.areEqualIgnoringOrder(list1, list2));
            assertFalse(CollectionUtil.areEqualIgnoringOrder(list1, list3));
        }

        @Test
        @DisplayName("Should find intersection of collections")
        void shouldFindIntersectionOfCollections() {
            List<String> list1 = Arrays.asList("a", "b", "c", "d");
            List<String> list2 = Arrays.asList("c", "d", "e", "f");
            
            List<String> intersection = CollectionUtil.intersection(list1, list2);
            
            assertThat(intersection).containsExactlyInAnyOrder("c", "d");
        }

        @Test
        @DisplayName("Should find union of collections")
        void shouldFindUnionOfCollections() {
            List<String> list1 = Arrays.asList("a", "b", "c");
            List<String> list2 = Arrays.asList("c", "d", "e");
            
            List<String> union = CollectionUtil.union(list1, list2);
            
            assertThat(union).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
        }

        @Test
        @DisplayName("Should find difference between collections")
        void shouldFindDifferenceBetweenCollections() {
            List<String> list1 = Arrays.asList("a", "b", "c", "d");
            List<String> list2 = Arrays.asList("c", "d", "e", "f");
            
            List<String> difference = CollectionUtil.difference(list1, list2);
            
            assertThat(difference).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("Should find symmetric difference of collections")
        void shouldFindSymmetricDifferenceOfCollections() {
            List<String> list1 = Arrays.asList("a", "b", "c", "d");
            List<String> list2 = Arrays.asList("c", "d", "e", "f");
            
            List<String> symmetricDiff = CollectionUtil.symmetricDifference(list1, list2);
            
            assertThat(symmetricDiff).containsExactlyInAnyOrder("a", "b", "e", "f");
        }
    }

    @Nested
    @DisplayName("Collection Aggregation Tests")
    class CollectionAggregationTests {

        @Test
        @DisplayName("Should sum numeric collection")
        void shouldSumNumericCollection() {
            List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
            
            int sum = CollectionUtil.sum(numbers);
            
            assertThat(sum).isEqualTo(15);
        }

        @Test
        @DisplayName("Should find min and max in collection")
        void shouldFindMinAndMaxInCollection() {
            List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
            
            Optional<Integer> min = CollectionUtil.min(numbers);
            Optional<Integer> max = CollectionUtil.max(numbers);
            
            assertThat(min).hasValue(1);
            assertThat(max).hasValue(9);
        }

        @Test
        @DisplayName("Should calculate average of numeric collection")
        void shouldCalculateAverageOfNumericCollection() {
            List<Double> numbers = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
            
            OptionalDouble average = CollectionUtil.average(numbers);
            
            assertThat(average).hasValue(3.0);
        }

        @Test
        @DisplayName("Should count occurrences of elements")
        void shouldCountOccurrencesOfElements() {
            List<String> words = Arrays.asList("apple", "banana", "apple", "orange", "banana", "apple");
            
            Map<String, Long> counts = CollectionUtil.countOccurrences(words);
            
            assertThat(counts.get("apple")).isEqualTo(3);
            assertThat(counts.get("banana")).isEqualTo(2);
            assertThat(counts.get("orange")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should find most frequent element")
        void shouldFindMostFrequentElement() {
            List<String> words = Arrays.asList("apple", "banana", "apple", "orange", "banana", "apple");
            
            Optional<String> mostFrequent = CollectionUtil.mostFrequent(words);
            
            assertThat(mostFrequent).hasValue("apple");
        }

        @Test
        @DisplayName("Should calculate statistics")
        void shouldCalculateStatistics() {
            List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            
            var stats = CollectionUtil.calculateStatistics(numbers);
            
            assertThat(stats.getCount()).isEqualTo(10);
            assertThat(stats.getSum()).isEqualTo(55);
            assertThat(stats.getMin()).isEqualTo(1);
            assertThat(stats.getMax()).isEqualTo(10);
            assertThat(stats.getAverage()).isEqualTo(5.5);
        }
    }

    @Nested
    @DisplayName("Collection Sorting Tests")
    class CollectionSortingTests {

        @Test
        @DisplayName("Should sort collection in natural order")
        void shouldSortCollectionInNaturalOrder() {
            List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
            
            List<Integer> sorted = CollectionUtil.sort(numbers);
            
            assertThat(sorted).containsExactly(1, 1, 2, 3, 4, 5, 6, 9);
        }

        @Test
        @DisplayName("Should sort collection with custom comparator")
        void shouldSortCollectionWithCustomComparator() {
            List<String> words = Arrays.asList("apple", "pie", "banana", "cake");
            
            List<String> sortedByLength = CollectionUtil.sort(words, 
                Comparator.comparing(String::length));
            
            assertThat(sortedByLength).containsExactly("pie", "cake", "apple", "banana");
        }

        @Test
        @DisplayName("Should sort collection in reverse order")
        void shouldSortCollectionInReverseOrder() {
            List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
            
            List<Integer> sortedDescending = CollectionUtil.sortDescending(numbers);
            
            assertThat(sortedDescending).containsExactly(9, 6, 5, 4, 3, 2, 1, 1);
        }

        @Test
        @DisplayName("Should get top N elements")
        void shouldGetTopNElements() {
            List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
            
            List<Integer> top3 = CollectionUtil.getTopN(numbers, 3);
            
            assertThat(top3).containsExactly(9, 6, 5);
        }

        @Test
        @DisplayName("Should get bottom N elements")
        void shouldGetBottomNElements() {
            List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
            
            List<Integer> bottom3 = CollectionUtil.getBottomN(numbers, 3);
            
            assertThat(bottom3).containsExactly(1, 1, 2);
        }
    }

    @Nested
    @DisplayName("Collection Validation Tests")
    class CollectionValidationTests {

        @Test
        @DisplayName("Should validate collection size")
        void shouldValidateCollectionSize() {
            List<String> list = Arrays.asList("a", "b", "c");
            
            assertTrue(CollectionUtil.hasSize(list, 3));
            assertFalse(CollectionUtil.hasSize(list, 5));
            assertFalse(CollectionUtil.hasSize(null, 0));
        }

        @Test
        @DisplayName("Should validate collection contains element")
        void shouldValidateCollectionContainsElement() {
            List<String> list = Arrays.asList("apple", "banana", "orange");
            
            assertTrue(CollectionUtil.contains(list, "banana"));
            assertFalse(CollectionUtil.contains(list, "grape"));
            assertFalse(CollectionUtil.contains(null, "anything"));
        }

        @Test
        @DisplayName("Should validate collection contains all elements")
        void shouldValidateCollectionContainsAllElements() {
            List<String> list = Arrays.asList("apple", "banana", "orange", "grape");
            List<String> subset = Arrays.asList("apple", "orange");
            List<String> nonSubset = Arrays.asList("apple", "kiwi");
            
            assertTrue(CollectionUtil.containsAll(list, subset));
            assertFalse(CollectionUtil.containsAll(list, nonSubset));
        }

        @Test
        @DisplayName("Should validate collection is sorted")
        void shouldValidateCollectionIsSorted() {
            List<Integer> sortedList = Arrays.asList(1, 2, 3, 4, 5);
            List<Integer> unsortedList = Arrays.asList(1, 3, 2, 4, 5);
            
            assertTrue(CollectionUtil.isSorted(sortedList));
            assertFalse(CollectionUtil.isSorted(unsortedList));
            assertTrue(CollectionUtil.isSorted(Collections.emptyList()));
            assertTrue(CollectionUtil.isSorted(Arrays.asList(42))); // Single element
        }

        @Test
        @DisplayName("Should validate collection has no duplicates")
        void shouldValidateCollectionHasNoDuplicates() {
            List<String> noDuplicates = Arrays.asList("a", "b", "c", "d");
            List<String> hasDuplicates = Arrays.asList("a", "b", "a", "d");
            
            assertTrue(CollectionUtil.hasNoDuplicates(noDuplicates));
            assertFalse(CollectionUtil.hasNoDuplicates(hasDuplicates));
        }
    }

    @Nested
    @DisplayName("Performance and Memory Tests")
    class PerformanceAndMemoryTests {

        @Test
        @DisplayName("Should efficiently process large collections")
        void shouldEfficientlyProcessLargeCollections() {
            List<Integer> largeList = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                largeList.add(i);
            }
            
            long startTime = System.currentTimeMillis();
            
            List<Integer> filtered = CollectionUtil.filter(largeList, n -> n % 2 == 0);
            List<String> mapped = CollectionUtil.map(filtered, Object::toString);
            
            long endTime = System.currentTimeMillis();
            
            assertThat(filtered).hasSize(50000);
            assertThat(mapped).hasSize(50000);
            assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("Should handle memory efficiently with streaming operations")
        void shouldHandleMemoryEfficientlyWithStreamingOperations() {
            List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            
            // Process elements one at a time to avoid loading all into memory
            long count = CollectionUtil.streamProcess(numbers)
                .filter(n -> n % 2 == 0)
                .mapToInt(Integer::intValue)
                .filter(n -> n > 4)
                .count();
            
            assertThat(count).isEqualTo(3); // 6, 8, 10
        }

        @ParameterizedTest
        @DisplayName("Should maintain consistent performance across different collection sizes")
        @ValueSource(ints = {10, 100, 1000, 10000})
        void shouldMaintainConsistentPerformanceAcrossDifferentSizes(int size) {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(i);
            }
            
            long startTime = System.nanoTime();
            
            boolean hasEvenNumbers = CollectionUtil.anyMatch(list, n -> n % 2 == 0);
            Optional<Integer> max = CollectionUtil.max(list);
            int sum = CollectionUtil.sum(list);
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertTrue(hasEvenNumbers);
            assertThat(max).hasValue(size - 1);
            assertThat(sum).isEqualTo((size - 1) * size / 2);
            
            // Performance should scale reasonably
            assertThat(durationMs).isLessThan(100);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle empty collections gracefully")
        void shouldHandleEmptyCollectionsGracefully() {
            List<String> emptyList = Collections.emptyList();
            
            assertThat(CollectionUtil.filter(emptyList, s -> s.length() > 0)).isEmpty();
            assertThat(CollectionUtil.map(emptyList, String::toUpperCase)).isEmpty();
            assertThat(CollectionUtil.findFirst(emptyList, s -> true)).isEmpty();
            assertFalse(CollectionUtil.anyMatch(emptyList, s -> true));
            assertTrue(CollectionUtil.allMatch(emptyList, s -> true)); // Vacuous truth
            assertTrue(CollectionUtil.noneMatch(emptyList, s -> true));
        }

        @Test
        @DisplayName("Should handle single element collections")
        void shouldHandleSingleElementCollections() {
            List<String> singleElement = Arrays.asList("only");
            
            assertThat(CollectionUtil.getFirst(singleElement)).isEqualTo("only");
            assertThat(CollectionUtil.getLast(singleElement)).isEqualTo("only");
            assertThat(CollectionUtil.reverse(singleElement)).containsExactly("only");
            assertThat(CollectionUtil.removeDuplicates(singleElement)).containsExactly("only");
        }

        @Test
        @DisplayName("Should handle collections with null elements")
        void shouldHandleCollectionsWithNullElements() {
            List<String> listWithNulls = Arrays.asList("a", null, "b", null, "c");
            
            assertThat(CollectionUtil.removeNulls(listWithNulls)).containsExactly("a", "b", "c");
            assertThat(CollectionUtil.filter(listWithNulls, Objects::nonNull)).containsExactly("a", "b", "c");
            assertThat(CollectionUtil.safeSize(listWithNulls)).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw appropriate exceptions for invalid operations")
        void shouldThrowAppropriateExceptionsForInvalidOperations() {
            List<String> list = Arrays.asList("a", "b", "c");
            
            assertThrows(IllegalArgumentException.class, () -> {
                CollectionUtil.chunk(list, 0); // Invalid chunk size
            });
            
            assertThrows(IllegalArgumentException.class, () -> {
                CollectionUtil.chunk(list, -1); // Negative chunk size
            });
            
            assertThrows(IllegalArgumentException.class, () -> {
                CollectionUtil.getTopN(list, -1); // Negative N
            });
        }

        @Test
        @DisplayName("Should handle concurrent modification gracefully")
        void shouldHandleConcurrentModificationGracefully() {
            List<Integer> synchronizedList = Collections.synchronizedList(new ArrayList<>());
            for (int i = 0; i < 1000; i++) {
                synchronizedList.add(i);
            }
            
            // This should not throw ConcurrentModificationException
            assertDoesNotThrow(() -> {
                CollectionUtil.safeIterate(synchronizedList, element -> {
                    // Simulate some processing
                    if (element % 100 == 0) {
                        Thread.yield();
                    }
                });
            });
        }
    }
}