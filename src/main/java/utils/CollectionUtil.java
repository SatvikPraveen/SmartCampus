// File location: src/main/java/utils/CollectionUtil.java
package utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for collection operations and manipulations
 * Provides enhanced collection functionality for academic data processing
 */
public final class CollectionUtil {
    
    private CollectionUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== NULL AND EMPTY CHECKS ====================
    
    /**
     * Checks if collection is null
     */
    public static boolean isNull(Collection<?> collection) {
        return collection == null;
    }
    
    /**
     * Checks if collection is not null
     */
    public static boolean isNotNull(Collection<?> collection) {
        return collection != null;
    }
    
    /**
     * Checks if collection is empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * Checks if collection is not empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
    
    /**
     * Checks if map is empty
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
    
    /**
     * Checks if map is not empty
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }
    
    /**
     * Checks if array is empty
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }
    
    /**
     * Checks if array is not empty
     */
    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }
    
    // ==================== SIZE OPERATIONS ====================
    
    /**
     * Gets size of collection safely
     */
    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }
    
    /**
     * Gets size of map safely
     */
    public static int size(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }
    
    /**
     * Gets length of array safely
     */
    public static int size(Object[] array) {
        return array == null ? 0 : array.length;
    }
    
    /**
     * Checks if collection has specific size
     */
    public static boolean hasSize(Collection<?> collection, int expectedSize) {
        return size(collection) == expectedSize;
    }
    
    /**
     * Checks if collection size is within range
     */
    public static boolean hasSizeBetween(Collection<?> collection, int minSize, int maxSize) {
        int actualSize = size(collection);
        return actualSize >= minSize && actualSize <= maxSize;
    }
    
    // ==================== DEFAULT VALUES ====================
    
    /**
     * Returns collection or empty list if null
     */
    public static <T> Collection<T> defaultIfNull(Collection<T> collection) {
        return collection == null ? new ArrayList<>() : collection;
    }
    
    /**
     * Returns list or empty list if null
     */
    public static <T> List<T> defaultIfNull(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }
    
    /**
     * Returns set or empty set if null
     */
    public static <T> Set<T> defaultIfNull(Set<T> set) {
        return set == null ? new HashSet<>() : set;
    }
    
    /**
     * Returns map or empty map if null
     */
    public static <K, V> Map<K, V> defaultIfNull(Map<K, V> map) {
        return map == null ? new HashMap<>() : map;
    }
    
    /**
     * Returns collection or default if empty
     */
    public static <T> Collection<T> defaultIfEmpty(Collection<T> collection, Collection<T> defaultCollection) {
        return isEmpty(collection) ? defaultCollection : collection;
    }
    
    // ==================== COLLECTION CREATION ====================
    
    /**
     * Creates list from varargs
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        if (elements == null || elements.length == 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(elements));
    }
    
    /**
     * Creates set from varargs
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        if (elements == null || elements.length == 0) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(elements));
    }
    
    /**
     * Creates map from key-value pairs
     */
    public static <K, V> Map<K, V> mapOf(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
    
    /**
     * Creates map from multiple key-value pairs
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    
    /**
     * Creates map from multiple key-value pairs
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
    
    /**
     * Creates immutable list from varargs
     */
    @SafeVarargs
    public static <T> List<T> immutableListOf(T... elements) {
        return Collections.unmodifiableList(listOf(elements));
    }
    
    /**
     * Creates immutable set from varargs
     */
    @SafeVarargs
    public static <T> Set<T> immutableSetOf(T... elements) {
        return Collections.unmodifiableSet(setOf(elements));
    }
    
    /**
     * Creates range of integers as list
     */
    public static List<Integer> range(int start, int end) {
        return IntStream.range(start, end).boxed().collect(Collectors.toList());
    }
    
    /**
     * Creates range of integers as list (inclusive)
     */
    public static List<Integer> rangeClosed(int start, int end) {
        return IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
    }
    
    // ==================== CONVERSION OPERATIONS ====================
    
    /**
     * Converts collection to list
     */
    public static <T> List<T> toList(Collection<T> collection) {
        if (collection == null) return new ArrayList<>();
        if (collection instanceof List) return (List<T>) collection;
        return new ArrayList<>(collection);
    }
    
    /**
     * Converts collection to set
     */
    public static <T> Set<T> toSet(Collection<T> collection) {
        if (collection == null) return new HashSet<>();
        if (collection instanceof Set) return (Set<T>) collection;
        return new HashSet<>(collection);
    }
    
    /**
     * Converts array to list
     */
    public static <T> List<T> toList(T[] array) {
        if (array == null) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(array));
    }
    
    /**
     * Converts array to set
     */
    public static <T> Set<T> toSet(T[] array) {
        if (array == null) return new HashSet<>();
        return new HashSet<>(Arrays.asList(array));
    }
    
    /**
     * Converts collection to array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Collection<T> collection, Class<T> type) {
        if (collection == null) {
            return (T[]) java.lang.reflect.Array.newInstance(type, 0);
        }
        return collection.toArray((T[]) java.lang.reflect.Array.newInstance(type, collection.size()));
    }
    
    // ==================== FILTERING OPERATIONS ====================
    
    /**
     * Filters collection by predicate
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return new ArrayList<>();
        return collection.stream().filter(predicate).collect(Collectors.toList());
    }
    
    /**
     * Filters and returns first element
     */
    public static <T> Optional<T> findFirst(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return Optional.empty();
        return collection.stream().filter(predicate).findFirst();
    }
    
    /**
     * Filters and returns any element
     */
    public static <T> Optional<T> findAny(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return Optional.empty();
        return collection.stream().filter(predicate).findAny();
    }
    
    /**
     * Removes null elements from collection
     */
    public static <T> List<T> removeNulls(Collection<T> collection) {
        return filter(collection, Objects::nonNull);
    }
    
    /**
     * Removes duplicates while preserving order
     */
    public static <T> List<T> removeDuplicates(Collection<T> collection) {
        if (collection == null) return new ArrayList<>();
        return collection.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * Removes duplicates by key extractor
     */
    public static <T, K> List<T> removeDuplicatesBy(Collection<T> collection, Function<T, K> keyExtractor) {
        if (collection == null) return new ArrayList<>();
        Set<K> seen = new HashSet<>();
        return collection.stream()
            .filter(item -> seen.add(keyExtractor.apply(item)))
            .collect(Collectors.toList());
    }
    
    // ==================== TRANSFORMATION OPERATIONS ====================
    
    /**
     * Maps collection to new type
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (collection == null) return new ArrayList<>();
        return collection.stream().map(mapper).collect(Collectors.toList());
    }
    
    /**
     * Flat maps collection
     */
    public static <T, R> List<R> flatMap(Collection<T> collection, Function<T, Collection<R>> mapper) {
        if (collection == null) return new ArrayList<>();
        return collection.stream()
            .flatMap(item -> defaultIfNull(mapper.apply(item)).stream())
            .collect(Collectors.toList());
    }
    
    /**
     * Maps with index
     */
    public static <T, R> List<R> mapWithIndex(List<T> list, BiFunction<T, Integer, R> mapper) {
        if (list == null) return new ArrayList<>();
        return IntStream.range(0, list.size())
            .mapToObj(i -> mapper.apply(list.get(i), i))
            .collect(Collectors.toList());
    }
    
    /**
     * Reverses collection
     */
    public static <T> List<T> reverse(Collection<T> collection) {
        List<T> list = toList(collection);
        Collections.reverse(list);
        return list;
    }
    
    /**
     * Sorts collection
     */
    public static <T extends Comparable<T>> List<T> sort(Collection<T> collection) {
        if (collection == null) return new ArrayList<>();
        return collection.stream().sorted().collect(Collectors.toList());
    }
    
    /**
     * Sorts collection with comparator
     */
    public static <T> List<T> sort(Collection<T> collection, Comparator<T> comparator) {
        if (collection == null) return new ArrayList<>();
        return collection.stream().sorted(comparator).collect(Collectors.toList());
    }
    
    /**
     * Shuffles collection
     */
    public static <T> List<T> shuffle(Collection<T> collection) {
        List<T> list = toList(collection);
        Collections.shuffle(list);
        return list;
    }
    
    /**
     * Shuffles collection with random
     */
    public static <T> List<T> shuffle(Collection<T> collection, Random random) {
        List<T> list = toList(collection);
        Collections.shuffle(list, random);
        return list;
    }
    
    // ==================== PARTITIONING OPERATIONS ====================
    
    /**
     * Partitions collection into chunks of specified size
     */
    public static <T> List<List<T>> partition(Collection<T> collection, int size) {
        if (collection == null || size <= 0) return new ArrayList<>();
        
        List<T> list = toList(collection);
        List<List<T>> partitions = new ArrayList<>();
        
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        
        return partitions;
    }
    
    /**
     * Partitions collection by predicate
     */
    public static <T> Map<Boolean, List<T>> partition(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) {
            return mapOf(true, new ArrayList<>(), false, new ArrayList<>());
        }
        return collection.stream().collect(Collectors.partitioningBy(predicate));
    }
    
    /**
     * Groups collection by classifier
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        if (collection == null) return new HashMap<>();
        return collection.stream().collect(Collectors.groupingBy(classifier));
    }
    
    /**
     * Groups collection by classifier with counting
     */
    public static <T, K> Map<K, Long> countBy(Collection<T> collection, Function<T, K> classifier) {
        if (collection == null) return new HashMap<>();
        return collection.stream().collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }
    
    // ==================== SLICING OPERATIONS ====================
    
    /**
     * Takes first n elements
     */
    public static <T> List<T> take(Collection<T> collection, int n) {
        if (collection == null || n <= 0) return new ArrayList<>();
        return collection.stream().limit(n).collect(Collectors.toList());
    }
    
    /**
     * Skips first n elements
     */
    public static <T> List<T> skip(Collection<T> collection, int n) {
        if (collection == null || n <= 0) return toList(collection);
        return collection.stream().skip(n).collect(Collectors.toList());
    }
    
    /**
     * Takes elements while predicate is true
     */
    public static <T> List<T> takeWhile(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return new ArrayList<>();
        return collection.stream().takeWhile(predicate).collect(Collectors.toList());
    }
    
    /**
     * Drops elements while predicate is true
     */
    public static <T> List<T> dropWhile(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return new ArrayList<>();
        return collection.stream().dropWhile(predicate).collect(Collectors.toList());
    }
    
    /**
     * Gets slice of collection
     */
    public static <T> List<T> slice(List<T> list, int fromIndex, int toIndex) {
        if (list == null) return new ArrayList<>();
        fromIndex = Math.max(0, fromIndex);
        toIndex = Math.min(list.size(), toIndex);
        if (fromIndex >= toIndex) return new ArrayList<>();
        return new ArrayList<>(list.subList(fromIndex, toIndex));
    }
    
    // ==================== ELEMENT ACCESS ====================
    
    /**
     * Gets first element
     */
    public static <T> Optional<T> first(Collection<T> collection) {
        if (isEmpty(collection)) return Optional.empty();
        return Optional.of(collection.iterator().next());
    }
    
    /**
     * Gets first element or default
     */
    public static <T> T firstOrDefault(Collection<T> collection, T defaultValue) {
        return first(collection).orElse(defaultValue);
    }
    
    /**
     * Gets last element
     */
    public static <T> Optional<T> last(Collection<T> collection) {
        if (isEmpty(collection)) return Optional.empty();
        
        if (collection instanceof List) {
            List<T> list = (List<T>) collection;
            return Optional.of(list.get(list.size() - 1));
        }
        
        T last = null;
        for (T item : collection) {
            last = item;
        }
        return Optional.ofNullable(last);
    }
    
    /**
     * Gets last element or default
     */
    public static <T> T lastOrDefault(Collection<T> collection, T defaultValue) {
        return last(collection).orElse(defaultValue);
    }
    
    /**
     * Gets element at index safely
     */
    public static <T> Optional<T> get(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.get(index));
    }
    
    /**
     * Gets element at index or default
     */
    public static <T> T getOrDefault(List<T> list, int index, T defaultValue) {
        return get(list, index).orElse(defaultValue);
    }
    
    /**
     * Gets random element
     */
    public static <T> Optional<T> random(Collection<T> collection) {
        if (isEmpty(collection)) return Optional.empty();
        
        List<T> list = toList(collection);
        int randomIndex = new Random().nextInt(list.size());
        return Optional.of(list.get(randomIndex));
    }
    
    /**
     * Gets random element with custom random
     */
    public static <T> Optional<T> random(Collection<T> collection, Random random) {
        if (isEmpty(collection)) return Optional.empty();
        
        List<T> list = toList(collection);
        int randomIndex = random.nextInt(list.size());
        return Optional.of(list.get(randomIndex));
    }
    
    // ==================== SET OPERATIONS ====================
    
    /**
     * Union of two collections
     */
    public static <T> Set<T> union(Collection<T> collection1, Collection<T> collection2) {
        Set<T> result = new HashSet<>(defaultIfNull(collection1));
        result.addAll(defaultIfNull(collection2));
        return result;
    }
    
    /**
     * Intersection of two collections
     */
    public static <T> Set<T> intersection(Collection<T> collection1, Collection<T> collection2) {
        if (isEmpty(collection1) || isEmpty(collection2)) return new HashSet<>();
        
        Set<T> result = new HashSet<>(collection1);
        result.retainAll(collection2);
        return result;
    }
    
    /**
     * Difference of two collections (elements in first but not in second)
     */
    public static <T> Set<T> difference(Collection<T> collection1, Collection<T> collection2) {
        if (isEmpty(collection1)) return new HashSet<>();
        
        Set<T> result = new HashSet<>(collection1);
        if (isNotEmpty(collection2)) {
            result.removeAll(collection2);
        }
        return result;
    }
    
    /**
     * Symmetric difference of two collections
     */
    public static <T> Set<T> symmetricDifference(Collection<T> collection1, Collection<T> collection2) {
        Set<T> diff1 = difference(collection1, collection2);
        Set<T> diff2 = difference(collection2, collection1);
        return union(diff1, diff2);
    }
    
    /**
     * Checks if collections are disjoint (no common elements)
     */
    public static <T> boolean disjoint(Collection<T> collection1, Collection<T> collection2) {
        if (isEmpty(collection1) || isEmpty(collection2)) return true;
        return Collections.disjoint(collection1, collection2);
    }
    
    // ==================== AGGREGATION OPERATIONS ====================
    
    /**
     * Counts elements matching predicate
     */
    public static <T> long count(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return 0;
        return collection.stream().filter(predicate).count();
    }
    
    /**
     * Checks if any element matches predicate
     */
    public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return false;
        return collection.stream().anyMatch(predicate);
    }
    
    /**
     * Checks if all elements match predicate
     */
    public static <T> boolean allMatch(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return true;
        return collection.stream().allMatch(predicate);
    }
    
    /**
     * Checks if no elements match predicate
     */
    public static <T> boolean noneMatch(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null) return true;
        return collection.stream().noneMatch(predicate);
    }
    
    /**
     * Finds minimum element
     */
    public static <T> Optional<T> min(Collection<T> collection, Comparator<T> comparator) {
        if (collection == null) return Optional.empty();
        return collection.stream().min(comparator);
    }
    
    /**
     * Finds maximum element
     */
    public static <T> Optional<T> max(Collection<T> collection, Comparator<T> comparator) {
        if (collection == null) return Optional.empty();
        return collection.stream().max(comparator);
    }
    
    /**
     * Reduces collection to single value
     */
    public static <T> Optional<T> reduce(Collection<T> collection, BinaryOperator<T> accumulator) {
        if (collection == null) return Optional.empty();
        return collection.stream().reduce(accumulator);
    }
    
    /**
     * Reduces collection with identity value
     */
    public static <T> T reduce(Collection<T> collection, T identity, BinaryOperator<T> accumulator) {
        if (collection == null) return identity;
        return collection.stream().reduce(identity, accumulator);
    }
    
    // ==================== JOINING OPERATIONS ====================
    
    /**
     * Joins collection elements with delimiter
     */
    public static String join(Collection<?> collection, String delimiter) {
        if (collection == null) return "";
        return collection.stream()
            .map(Object::toString)
            .collect(Collectors.joining(delimiter));
    }
    
    /**
     * Joins collection elements with delimiter, prefix, and suffix
     */
    public static String join(Collection<?> collection, String delimiter, String prefix, String suffix) {
        if (collection == null) return prefix + suffix;
        return collection.stream()
            .map(Object::toString)
            .collect(Collectors.joining(delimiter, prefix, suffix));
    }
    
    /**
     * Joins with comma
     */
    public static String joinWithComma(Collection<?> collection) {
        return join(collection, ", ");
    }
    
    /**
     * Joins filtered elements
     */
    public static <T> String joinFiltered(Collection<T> collection, Predicate<T> filter, String delimiter) {
        if (collection == null) return "";
        return collection.stream()
            .filter(filter)
            .map(Object::toString)
            .collect(Collectors.joining(delimiter));
    }
    
    // ==================== FREQUENCY OPERATIONS ====================
    
    /**
     * Counts frequency of each element
     */
    public static <T> Map<T, Long> frequency(Collection<T> collection) {
        if (collection == null) return new HashMap<>();
        return collection.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
    
    /**
     * Gets most frequent element
     */
    public static <T> Optional<T> mostFrequent(Collection<T> collection) {
        Map<T, Long> frequencies = frequency(collection);
        return frequencies.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }
    
    /**
     * Gets least frequent element
     */
    public static <T> Optional<T> leastFrequent(Collection<T> collection) {
        Map<T, Long> frequencies = frequency(collection);
        return frequencies.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Swaps elements at two indices in a list
     */
    public static <T> void swap(List<T> list, int i, int j) {
        if (list != null && i >= 0 && i < list.size() && j >= 0 && j < list.size()) {
            Collections.swap(list, i, j);
        }
    }
    
    /**
     * Rotates list by distance
     */
    public static <T> List<T> rotate(List<T> list, int distance) {
        if (list == null) return new ArrayList<>();
        List<T> rotated = new ArrayList<>(list);
        Collections.rotate(rotated, distance);
        return rotated;
    }
    
    /**
     * Fills collection with value
     */
    public static <T> List<T> fill(int size, T value) {
        return Collections.nCopies(size, value);
    }
    
    /**
     * Creates list by repeating collection
     */
    public static <T> List<T> repeat(Collection<T> collection, int times) {
        if (collection == null || times <= 0) return new ArrayList<>();
        
        List<T> result = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            result.addAll(collection);
        }
        return result;
    }
    
    /**
     * Checks if collection contains all elements from another collection
     */
    public static <T> boolean containsAll(Collection<T> collection, Collection<T> elements) {
        if (collection == null) return isEmpty(elements);
        if (elements == null) return true;
        return collection.containsAll(elements);
    }
    
    /**
     * Checks if collection contains any element from another collection
     */
    public static <T> boolean containsAny(Collection<T> collection, Collection<T> elements) {
        if (isEmpty(collection) || isEmpty(elements)) return false;
        return elements.stream().anyMatch(collection::contains);
    }
    
    /**
     * Adds all elements to collection safely
     */
    public static <T> boolean addAll(Collection<T> target, Collection<T> source) {
        if (target == null || source == null) return false;
        return target.addAll(source);
    }
    
    /**
     * Removes all elements from collection safely
     */
    public static <T> boolean removeAll(Collection<T> target, Collection<T> toRemove) {
        if (target == null || toRemove == null) return false;
        return target.removeAll(toRemove);
    }
    
    /**
     * Retains only elements present in another collection
     */
    public static <T> boolean retainAll(Collection<T> target, Collection<T> toRetain) {
        if (target == null || toRetain == null) return false;
        return target.retainAll(toRetain);
    }
}