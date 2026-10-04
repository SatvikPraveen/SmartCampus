package utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class CollectionUtilTest {

    @Nested
    class NullSafety {
        @Test
        void emptinessAndSize() {
            assertThat(CollectionUtil.isEmpty((List<?>) null)).isTrue();
            assertThat(CollectionUtil.isEmpty(List.of())).isTrue();
            assertThat(CollectionUtil.isNotEmpty(List.of(1))).isTrue();
            assertThat(CollectionUtil.isEmpty((Map<?, ?>) null)).isTrue();
            assertThat(CollectionUtil.isNotEmpty(Map.of(1, 2))).isTrue();
            assertThat(CollectionUtil.isEmpty(new Object[0])).isTrue();
            assertThat(CollectionUtil.isNotEmpty(new Object[]{1})).isTrue();
            assertThat(CollectionUtil.size((List<?>) null)).isZero();
            assertThat(CollectionUtil.size((Map<?, ?>) null)).isZero();
            assertThat(CollectionUtil.size((Object[]) null)).isZero();
            assertThat(CollectionUtil.size(new Object[3])).isEqualTo(3);
            assertThat(CollectionUtil.isNull(null)).isTrue();
            assertThat(CollectionUtil.isNotNull(List.of())).isTrue();
        }

        @Test
        void sizeChecks() {
            assertThat(CollectionUtil.hasSize(List.of(1, 2), 2)).isTrue();
            assertThat(CollectionUtil.hasSize(null, 0)).isTrue();
            assertThat(CollectionUtil.hasSizeBetween(List.of(1, 2), 1, 2)).isTrue();
            assertThat(CollectionUtil.hasSizeBetween(List.of(1, 2, 3), 1, 2)).isFalse();
        }

        @Test
        void defaults() {
            List<Integer> list = List.of(1);
            assertThat(CollectionUtil.defaultIfNull(list)).isSameAs(list);
            assertThat(CollectionUtil.defaultIfNull((List<Integer>) null)).isEmpty();
            assertThat(CollectionUtil.defaultIfNull((Set<Integer>) null)).isEmpty();
            assertThat(CollectionUtil.defaultIfNull((Map<Integer, Integer>) null)).isEmpty();
            assertThat(CollectionUtil.defaultIfEmpty(List.of(), List.of(9))).containsExactly(9);
            assertThat(CollectionUtil.defaultIfEmpty(List.of(1), List.of(9))).containsExactly(1);
        }
    }

    @Nested
    class Creation {
        @Test
        void mutableFactories() {
            List<String> list = CollectionUtil.listOf("a", "b");
            list.add("c");
            assertThat(list).containsExactly("a", "b", "c");
            Set<String> set = CollectionUtil.setOf("a", "a", "b");
            assertThat(set).containsExactlyInAnyOrder("a", "b");
            assertThat(CollectionUtil.listOf()).isEmpty();
            assertThat(CollectionUtil.mapOf("k", 1)).containsExactly(entry("k", 1));
            assertThat(CollectionUtil.mapOf("a", 1, "b", 2)).hasSize(2);
            assertThat(CollectionUtil.mapOf("a", 1, "b", 2, "c", 3)).containsEntry("c", 3);
        }

        @Test
        void immutableFactoriesRejectModification() {
            List<String> list = CollectionUtil.immutableListOf("a");
            Set<String> set = CollectionUtil.immutableSetOf("a");
            assertThatThrownBy(() -> list.add("b")).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> set.add("b")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void ranges() {
            assertThat(CollectionUtil.range(1, 4)).containsExactly(1, 2, 3);
            assertThat(CollectionUtil.rangeClosed(1, 4)).containsExactly(1, 2, 3, 4);
            assertThat(CollectionUtil.range(4, 1)).isEmpty();
        }
    }

    @Nested
    class Conversion {
        @Test
        void conversions() {
            Set<Integer> treeSet = new TreeSet<>(List.of(3, 1, 2));
            assertThat(CollectionUtil.toList(treeSet)).containsExactly(1, 2, 3);
            assertThat(CollectionUtil.toSet(List.of(1, 1, 2))).containsExactlyInAnyOrder(1, 2);
            assertThat(CollectionUtil.toList(new Integer[]{1, 2})).containsExactly(1, 2);
            assertThat(CollectionUtil.toSet(new Integer[]{1, 1})).containsExactly(1);
            assertThat(CollectionUtil.toList((List<Integer>) null)).isEmpty();
            assertThat(CollectionUtil.toArray(List.of("a", "b"), String.class)).containsExactly("a", "b");
            assertThat(CollectionUtil.toArray(null, String.class)).isEmpty();
        }
    }

    @Nested
    class FilteringAndTransforming {
        private final List<Integer> nums = List.of(5, 3, 8, 1, 8, 2);

        @Test
        void filterAndFind() {
            assertThat(CollectionUtil.filter(nums, n -> n > 4)).containsExactly(5, 8, 8);
            assertThat(CollectionUtil.findFirst(nums, n -> n > 5)).contains(8);
            assertThat(CollectionUtil.findFirst(nums, n -> n > 100)).isEmpty();
            assertThat(CollectionUtil.findAny(nums, n -> n == 3)).contains(3);
            assertThat(CollectionUtil.filter(null, n -> true)).isEmpty();
        }

        @Test
        void nullsAndDuplicates() {
            assertThat(CollectionUtil.removeNulls(Arrays.asList(1, null, 2, null))).containsExactly(1, 2);
            assertThat(CollectionUtil.removeDuplicates(nums)).containsExactly(5, 3, 8, 1, 2);
            assertThat(CollectionUtil.removeDuplicatesBy(List.of("apple", "avocado", "banana", "blueberry", "cherry"),
                s -> s.charAt(0))).containsExactly("apple", "banana", "cherry");
        }

        @Test
        void mapping() {
            assertThat(CollectionUtil.map(List.of("a", "bb"), String::length)).containsExactly(1, 2);
            assertThat(CollectionUtil.flatMap(List.of(1, 2, 3), n -> n == 2 ? null : List.of(n, n)))
                .containsExactly(1, 1, 3, 3);
            assertThat(CollectionUtil.mapWithIndex(List.of("a", "b"), (s, i) -> i + s)).containsExactly("0a", "1b");
        }

        @Test
        void sorting() {
            assertThat(CollectionUtil.sort(nums)).containsExactly(1, 2, 3, 5, 8, 8);
            assertThat(CollectionUtil.sort(nums, Comparator.reverseOrder())).containsExactly(8, 8, 5, 3, 2, 1);
            assertThat(nums).containsExactly(5, 3, 8, 1, 8, 2);
        }

        @Test
        void reverseReturnsNewListAndLeavesInputUntouched() {
            List<Integer> input = new ArrayList<>(List.of(1, 2, 3));
            List<Integer> reversed = CollectionUtil.reverse(input);
            assertThat(reversed).containsExactly(3, 2, 1);
            assertThat(input).containsExactly(1, 2, 3);
            assertThat(CollectionUtil.reverse(List.of(1, 2))).containsExactly(2, 1);
            assertThat(CollectionUtil.reverse(null)).isEmpty();
        }

        @Test
        void shuffleIsAPermutationAndLeavesInputUntouched() {
            List<Integer> input = List.copyOf(CollectionUtil.range(0, 50));
            List<Integer> a = CollectionUtil.shuffle(input, new Random(42));
            List<Integer> b = CollectionUtil.shuffle(input, new Random(42));
            assertThat(a).isEqualTo(b).containsExactlyInAnyOrderElementsOf(input).isNotEqualTo(input);
            List<Integer> mutable = new ArrayList<>(input);
            CollectionUtil.shuffle(mutable, new Random(7));
            CollectionUtil.shuffle(mutable);
            assertThat(mutable).isEqualTo(input);
        }
    }

    @Nested
    class Partitioning {
        @Test
        void chunks() {
            List<List<Integer>> parts = CollectionUtil.partition(CollectionUtil.rangeClosed(1, 7), 3);
            assertThat(parts).containsExactly(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7));
            assertThat(CollectionUtil.partition(List.of(1), 0)).isEmpty();
            assertThat(CollectionUtil.partition((List<Integer>) null, 2)).isEmpty();
        }

        @Test
        void chunksAreIndependentOfTheSource() {
            List<Integer> source = new ArrayList<>(List.of(1, 2, 3, 4));
            List<List<Integer>> parts = CollectionUtil.partition(source, 2);
            source.add(5);
            assertThat(parts).containsExactly(List.of(1, 2), List.of(3, 4));
        }

        @Test
        void byPredicateAndGrouping() {
            Map<Boolean, List<Integer>> evenOdd = CollectionUtil.partition(List.of(1, 2, 3, 4), n -> n % 2 == 0);
            assertThat(evenOdd.get(true)).containsExactly(2, 4);
            assertThat(evenOdd.get(false)).containsExactly(1, 3);
            Map<Boolean, List<Integer>> empty = CollectionUtil.partition((List<Integer>) null, n -> true);
            assertThat(empty.get(true)).isEmpty();
            assertThat(empty.get(false)).isEmpty();
            assertThat(CollectionUtil.groupBy(List.of("a", "bb", "cc"), String::length))
                .containsEntry(1, List.of("a")).containsEntry(2, List.of("bb", "cc"));
            assertThat(CollectionUtil.countBy(List.of("a", "bb", "cc"), String::length))
                .containsEntry(1, 1L).containsEntry(2, 2L);
        }
    }

    @Nested
    class Slicing {
        private final List<Integer> nums = List.of(1, 2, 3, 4, 5);

        @Test
        void takeSkip() {
            assertThat(CollectionUtil.take(nums, 2)).containsExactly(1, 2);
            assertThat(CollectionUtil.take(nums, 10)).containsExactlyElementsOf(nums);
            assertThat(CollectionUtil.take(nums, -1)).isEmpty();
            assertThat(CollectionUtil.skip(nums, 3)).containsExactly(4, 5);
            assertThat(CollectionUtil.skip(nums, 0)).containsExactlyElementsOf(nums);
            assertThat(CollectionUtil.skip(nums, 10)).isEmpty();
        }

        @Test
        void takeWhileDropWhile() {
            assertThat(CollectionUtil.takeWhile(nums, n -> n < 3)).containsExactly(1, 2);
            assertThat(CollectionUtil.dropWhile(nums, n -> n < 3)).containsExactly(3, 4, 5);
        }

        @ParameterizedTest
        @CsvSource({"1, 3, '2,3'", "-5, 2, '1,2'", "3, 99, '4,5'", "3, 3, ''", "4, 2, ''"})
        void sliceClampsIndices(int from, int to, String expected) {
            List<String> actual = CollectionUtil.map(CollectionUtil.slice(nums, from, to), String::valueOf);
            assertThat(String.join(",", actual)).isEqualTo(expected);
        }
    }

    @Nested
    class ElementAccess {
        @Test
        void firstAndLast() {
            assertThat(CollectionUtil.first(List.of(1, 2))).contains(1);
            assertThat(CollectionUtil.last(List.of(1, 2))).contains(2);
            assertThat(CollectionUtil.last(new LinkedHashSet<>(List.of(1, 2, 3)))).contains(3);
            assertThat(CollectionUtil.first(List.of())).isEmpty();
            assertThat(CollectionUtil.firstOrDefault(null, 9)).isEqualTo(9);
            assertThat(CollectionUtil.lastOrDefault(List.of(), 9)).isEqualTo(9);
        }

        @Test
        void nullElementsAtTheEndsDoNotThrow() {
            List<String> list = Arrays.asList(null, "x", null);
            assertThat(CollectionUtil.first(list)).isEmpty();
            assertThat(CollectionUtil.last(list)).isEmpty();
            assertThat(CollectionUtil.firstOrDefault(list, "d")).isEqualTo("d");
            assertThat(CollectionUtil.lastOrDefault(list, "d")).isEqualTo("d");
        }

        @Test
        void indexAccess() {
            List<String> list = List.of("a", "b");
            assertThat(CollectionUtil.get(list, 1)).contains("b");
            assertThat(CollectionUtil.get(list, 2)).isEmpty();
            assertThat(CollectionUtil.get(list, -1)).isEmpty();
            assertThat(CollectionUtil.getOrDefault(list, 5, "z")).isEqualTo("z");
        }

        @Test
        void randomElementIsMember() {
            List<Integer> list = List.of(10, 20, 30);
            assertThat(CollectionUtil.random(list, new Random(1))).get().isIn(list);
            assertThat(CollectionUtil.random(list)).get().isIn(list);
            assertThat(CollectionUtil.random(List.of(), new Random(1))).isEmpty();
        }
    }

    @Nested
    class SetOperations {
        private final List<Integer> a = List.of(1, 2, 3, 4);
        private final List<Integer> b = List.of(3, 4, 5);

        @Test
        void algebra() {
            assertThat(CollectionUtil.union(a, b)).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
            assertThat(CollectionUtil.intersection(a, b)).containsExactlyInAnyOrder(3, 4);
            assertThat(CollectionUtil.difference(a, b)).containsExactlyInAnyOrder(1, 2);
            assertThat(CollectionUtil.symmetricDifference(a, b)).containsExactlyInAnyOrder(1, 2, 5);
            assertThat(CollectionUtil.disjoint(a, b)).isFalse();
            assertThat(CollectionUtil.disjoint(a, List.of(9))).isTrue();
        }

        @Test
        void identitiesWithNullAndEmpty() {
            assertThat(CollectionUtil.union(a, null)).containsExactlyInAnyOrderElementsOf(a);
            assertThat(CollectionUtil.intersection(a, null)).isEmpty();
            assertThat(CollectionUtil.difference(a, null)).containsExactlyInAnyOrderElementsOf(a);
            assertThat(CollectionUtil.difference(null, a)).isEmpty();
            assertThat(CollectionUtil.disjoint(null, a)).isTrue();
        }

        @Test
        void unionSizeEqualsInclusionExclusion() {
            int union = CollectionUtil.union(a, b).size();
            int inter = CollectionUtil.intersection(a, b).size();
            assertThat(union).isEqualTo(a.size() + b.size() - inter);
        }
    }

    @Nested
    class Aggregation {
        private final List<Integer> nums = List.of(4, 7, 1, 9);

        @Test
        void predicates() {
            assertThat(CollectionUtil.count(nums, n -> n > 3)).isEqualTo(3);
            assertThat(CollectionUtil.anyMatch(nums, n -> n == 9)).isTrue();
            assertThat(CollectionUtil.allMatch(nums, n -> n > 0)).isTrue();
            assertThat(CollectionUtil.noneMatch(nums, n -> n > 9)).isTrue();
            assertThat(CollectionUtil.anyMatch(null, n -> true)).isFalse();
            assertThat(CollectionUtil.allMatch(null, n -> false)).isTrue();
            assertThat(CollectionUtil.noneMatch(null, n -> true)).isTrue();
            assertThat(CollectionUtil.count(null, n -> true)).isZero();
        }

        @Test
        void minMaxReduce() {
            assertThat(CollectionUtil.min(nums, Comparator.naturalOrder())).contains(1);
            assertThat(CollectionUtil.max(nums, Comparator.naturalOrder())).contains(9);
            assertThat(CollectionUtil.reduce(nums, Integer::sum)).contains(21);
            assertThat(CollectionUtil.reduce(nums, 100, Integer::sum)).isEqualTo(121);
            assertThat(CollectionUtil.reduce((List<Integer>) null, 100, Integer::sum)).isEqualTo(100);
            assertThat(CollectionUtil.reduce((List<Integer>) null, Integer::sum)).isEmpty();
        }

        @Test
        void joining() {
            assertThat(CollectionUtil.join(List.of(1, 2, 3), "-")).isEqualTo("1-2-3");
            assertThat(CollectionUtil.join(List.of(1, 2), ", ", "[", "]")).isEqualTo("[1, 2]");
            assertThat(CollectionUtil.join(null, ", ", "[", "]")).isEqualTo("[]");
            assertThat(CollectionUtil.joinWithComma(List.of("a", "b"))).isEqualTo("a, b");
            assertThat(CollectionUtil.joinFiltered(nums, n -> n > 4, "|")).isEqualTo("7|9");
            assertThat(CollectionUtil.join(null, ",")).isEmpty();
        }

        @Test
        void frequency() {
            List<String> words = List.of("a", "b", "a", "c", "a", "b");
            assertThat(CollectionUtil.frequency(words)).containsOnly(entry("a", 3L), entry("b", 2L), entry("c", 1L));
            assertThat(CollectionUtil.mostFrequent(words)).contains("a");
            assertThat(CollectionUtil.leastFrequent(words)).contains("c");
            assertThat(CollectionUtil.mostFrequent(null)).isEmpty();
        }
    }

    @Nested
    class Utilities {
        @Test
        void swapIgnoresOutOfRangeIndices() {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
            CollectionUtil.swap(list, 0, 2);
            assertThat(list).containsExactly(3, 2, 1);
            CollectionUtil.swap(list, 0, 5);
            assertThat(list).containsExactly(3, 2, 1);
        }

        @Test
        void rotateReturnsCopy() {
            List<Integer> list = List.of(1, 2, 3, 4);
            assertThat(CollectionUtil.rotate(list, 1)).containsExactly(4, 1, 2, 3);
            assertThat(CollectionUtil.rotate(list, -1)).containsExactly(2, 3, 4, 1);
            assertThat(CollectionUtil.rotate(list, 4)).containsExactlyElementsOf(list);
        }

        @Test
        void fillAndRepeat() {
            assertThat(CollectionUtil.fill(3, "x")).containsExactly("x", "x", "x");
            assertThat(CollectionUtil.repeat(List.of(1, 2), 2)).containsExactly(1, 2, 1, 2);
            assertThat(CollectionUtil.repeat(List.of(1), 0)).isEmpty();
        }

        @Test
        void containment() {
            assertThat(CollectionUtil.containsAll(List.of(1, 2, 3), List.of(1, 3))).isTrue();
            assertThat(CollectionUtil.containsAll(List.of(1), List.of(1, 3))).isFalse();
            assertThat(CollectionUtil.containsAll(null, List.of())).isTrue();
            assertThat(CollectionUtil.containsAll(null, List.of(1))).isFalse();
            assertThat(CollectionUtil.containsAll(List.of(1), null)).isTrue();
            assertThat(CollectionUtil.containsAny(List.of(1, 2), List.of(5, 2))).isTrue();
            assertThat(CollectionUtil.containsAny(List.of(1, 2), List.of(5))).isFalse();
            assertThat(CollectionUtil.containsAny(null, List.of(5))).isFalse();
        }

        @Test
        void bulkMutators() {
            List<Integer> target = new ArrayList<>(List.of(1, 2, 3));
            assertThat(CollectionUtil.addAll(target, List.of(4))).isTrue();
            assertThat(CollectionUtil.removeAll(target, List.of(1))).isTrue();
            assertThat(CollectionUtil.retainAll(target, List.of(2, 4))).isTrue();
            assertThat(target).containsExactly(2, 4);
            assertThat(CollectionUtil.addAll(null, List.of(1))).isFalse();
            assertThat(CollectionUtil.removeAll(target, null)).isFalse();
            assertThat(CollectionUtil.retainAll(null, target)).isFalse();
        }
    }
}
